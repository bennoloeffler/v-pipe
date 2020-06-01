package transform

import model.DataReader
import model.Model
import model.PipelineOriginalElement
import model.TaskInProject
import model.VpipeDataException
import groovy.time.TimeCategory
import groovy.time.TimeDuration
import groovy.transform.Immutable
import groovy.transform.InheritConstructors


/**
 * Elements after automatic pipelining.
 */
@Immutable
class PipelineCalcElement {
    Date startDateCalc
    Date endDateCalc
    PipelineOriginalElement originalElement

    String toString() { // for the Dates to be readable
        "Pipline-Ergebnis(${startDateCalc.toString()} ${endDateCalc.toString()} <-- ${originalElement.toString()})"
    }

}

/**
 * A pipeline "stacks" the tasks one after each other.
 * It is important, that the space is filled in the sequence of adding tasks.
 * The first tasks serve as "time anchor" in the sense, that they are not shifted.
 * The just find a slot without getting moved in time.
 * All other slots are moved - to the next free slot place in direction future.
 *
 * If a task uses more than one slot, is is split without reflecting the width of spread.
 * But at the end, the percentage increase of the spread parts in relation to the original
 * task is calculated - the max of all found.
 * That may serve as an indicator, if the spread is acceptable.
 * E.g. if a slot-consumption of 4 from a task is intended and 4 slots are available.
 * BUT: 3 very long other tasks are blocking 3 slots.
 * That may result in an increase of length of 300%
 */
class Pipeline {

    /**
     * if a 100 days task is split to three parallel and they are
     * shifting e.g. 30 days (because the algorithm does not even try) then:
     * the result is:
     * 0.3
     */
    double increaseTaskLenDueToParallel = 0

    /**
     * Pipeline-Number is the key.
     * Each Pipeline consists of a list of PipelineCalcElements
     */
    Map<Integer, List<PipelineCalcElement>> pipeline = [:]


    /**
     *
     * @param slotsInPipeline
     */
    Pipeline(int slotsInPipeline) {
        // create the pipelines
        (0..slotsInPipeline-1).each {
            pipeline[it] = []
        }
    }


    /**
     * @return the next slot is the one with the next earliest date
     */
    int nextSlot() {
        // first search if empty pipeline exists...
        for (entry in pipeline) {
            if(entry.value.empty) {return entry.key}
        }

        //then, search for next earliest slot-number
        def min = pipeline.entrySet().min() {
            it.value.last().endDateCalc
        }
        min.key
    }


    /**
     * Find the next free slot and put Pipline-Element there.
     * Or distribute it, when it occupies more that one slot.
     * @param poe
     */
    def addNext(PipelineOriginalElement poe) {
        if (poe.pipelineSlotsNeeded > pipeline.size()) {
            throw new VpipeDataException("Staffelungs-Element braucht mehr Slots ($poe.pipelineSlotsNeeded)\nals verfügbar (${pipeline.size()}).\n${poe.toString()}")
        }

        List<PipelineCalcElement> shiftList = []

        poe.pipelineSlotsNeeded.times {
            int s = nextSlot()
            Date startNext = pipeline[s] ? pipeline[s].last().endDateCalc : poe.startDate
            use(TimeCategory) {
                Date endNext = startNext + (poe.endDate - poe.startDate)
                def pce = new PipelineCalcElement(startDateCalc: startNext, endDateCalc: endNext, originalElement: poe)
                pipeline[s] << pce
                shiftList << pce
            }
        }
        if(shiftList.size() > 1) {
            def earliest = shiftList*.startDateCalc.min()
            def latest = shiftList*.endDateCalc.max()
            use(TimeCategory) {
                def durOriginal = poe.endDate - poe.startDate
                def durShifted = latest - earliest
                increaseTaskLenDueToParallel = Math.max(
                        ((durShifted - durOriginal).days as double) / durOriginal.days,
                        increaseTaskLenDueToParallel)
            }
        }
    }


    /**
     * @return how much the projects shifted because of the pipelining
     */
    Map<String, Integer> getProjectShift() {
        Map<String, Integer> r = [:]
        pipeline.each() { entry ->
            entry.value.each { pce ->
                TimeDuration dur
                use(TimeCategory) {
                    dur = pce.endDateCalc - pce.originalElement.endDate
                }
                if (r[pce.originalElement.project]) { // if there is already an entry...
                    r[pce.originalElement.project] = Math.max(dur.getDays(), r[pce.originalElement.project])
                    // take latest
                } else {
                    r[pce.originalElement.project] = dur.getDays()
                }
            }
        }
        return r
    }


}


@InheritConstructors
class PipelineTransformer extends Transformer {


    /**
     * @return the model.TaskInProject List that is transformed
     */
    @Override
    Model transform() {

        assert model != null

        if(maxPipelineSlots == 0) { // DO NO TRANSFORM
            return model
        }

        description="Dates transformed:\n"
        List<TaskInProject> result = []

        // first of all, do the pipelining
        Pipeline p = new Pipeline(maxPipelineSlots)
        pipelineElements.each {
            p.addNext(it)
        }
        Map<String, Integer> projectDayShift = p.getProjectShift()

        def allProjects = getAllProjects()
        allProjects.each() { projectName ->
            def projectList = getProject(projectName)
            int shift = projectDayShift[projectName]?:0
            if(shift){description += "$projectName: $shift\n"}
            projectList.each() {
                result << new TaskInProject(it.project, it.starting + shift, it.ending + shift, it.department, it.capacityNeeded)
            }
        }

        // Check: for every pipeline-Element, there needs to be a real project - and vice verca
        projectDayShift.keySet().each {
            if(! getProject(it)) {
                throw new VpipeDataException("$DataReader.PIPELINING_FILE_NAME enthält Projekte,\ndie nicht in den Grunddaten sind: $it")
            }
        }
        getAllProjects().each {
            def projects = pipelineElements*.project
            if (! projects.contains(it)) {
                throw new VpipeDataException("Grunddaten enthalten Projekte,\ndie nicht in $DataReader.PIPELINING_FILE_NAME aufgeführt sind: $it")
            }
        }

        if (p.increaseTaskLenDueToParallel > 0) {
            def warn ="Verlängerung eines Tasks wegen Aufspaltung und Verschiebung in der Pipeline um ${p.increaseTaskLenDueToParallel*100 as int}%"
            println "V O R S I C H T :  $warn"
            description += warn
        }


        if (description == "Dates transformed:\n") {description+"none..."}

        //Model clone = model.clone() as Model
        model.taskList = result
        model
    }


}