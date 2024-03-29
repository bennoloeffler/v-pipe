package learn.swingbuilder

import groovy.swing.SwingBuilder

import java.awt.BorderLayout
import javax.swing.JFrame
import java.awt.GridLayout
import java.awt.Color

class Multithreading {
    static SwingBuilder sb = new SwingBuilder()

    static void main(String[] args) {


        sb.build {
            def myFrame = frame(title: 'SwingThreading', pack: true, defaultCloseOperation: JFrame.EXIT_ON_CLOSE) {
                borderLayout()
                def lazyPanelsParent
                scrollPane(constraints: BorderLayout.CENTER, preferredSize: [500, 300]) {
                    lazyPanelsParent = panel(layout: new GridLayout(0, 1, 5, 5)) {}
                }
                panel(constraints: BorderLayout.NORTH) {
                    button(text: 'Build One', actionPerformed: {
                        println 'Pressed'
                        buildInTheBackground(lazyPanelsParent)
                    })
                }
            }
            myFrame.show()
        }
    }

/**
 * Create a Swing panel in the background using SwingBuilder.
 * This can be called from any thread.*/
    static def buildInTheBackground(parentPanel) {
        // If we are not already on the EDT, static SwingBuilder.build(Closure) will do that for us.
        // In the case of an event handler like the actionPerformed for the button, then naturally
        // we're on the EDT already and the building will continue immediately.
        sb.build {
            def statusMessage = label(border: lineBorder(color: Color.RED, thickness: 4))

            // Notice that the parameter to setStatus() is declared as String.
            // That way if code uses a GString it gets rendered at the time (and on the
            // thread) of the call.  If we don't do that then it might get deferred,
            // by which time the results that the GString will produce may change and
            // could be in a race (results varying with execution order and maybe invalid).
            // Also the context here is that the caller is the work environment and that
            // operations on the EDT are to be quick and determinate for display only.

            def setStatus = { String msg -> println msg; statusMessage.setText(msg) }

            // We can change the shown Swing tree here because we're on the EDT.

            setStatus('Building...')

            // Put the message at the top of the list if we use index = 0.
            // Use plain parentPanel.add(statusMessage) if you want the end of the list.
            parentPanel.add(statusMessage, 0)

            // Component.validate() must be called after any changes that affect layout
            // on realized (shown) AWT/Swing trees.
            parentPanel.validate()

            doOutside {
                // Now we're off on our own thread.

                // To change the display, be sure to run that code on the EDT.
                edt { setStatus('Working outside') }

                // That will wait, which isn't really necessary here.
                // For more parallelism, use doLater.
                // doLater { setStatus('Working outside') }

                // Here we do our lengthy work.
                sleep(3000)
                def powerData = [*0..5].collect { power -> [*0..10].collect { Math.pow(it, power) } }

                // SwingBuilding may take a while too.
                def newPanel = panel(minimumSize: [250, 250], border: lineBorder(color: Color.BLUE, thickness: 2)) {
                    edt { setStatus("Building new panel") }

                    sleep(3000)
                    tableLayout() {
                        powerData.each { row -> tr { row.each { cell -> td { textField(cell.toString()) } } } }
                    }
                }

                edt { setStatus("New panel ready") }

                // Back to the EDT for fiddling with the Swing tree.

                doLater {
                    setStatus("Adding new panel")

                    // We're gonna remove the widget used for the new panel's progress indication.
                    // An alternate (and simpler) way to get this effect is to put the status
                    // in a panel that doesn't get removed.  But this method is shown because
                    // it is fairly general in that the status indicator can have a utility
                    // layout that gets removed when done and so doesn't impact the customized
                    // structure desired for the application.

                    // We may not be the zeroth child (beginning of list) anymore.
                    // (Or end of list either, if that's where we started.)
                    def idx = (parentPanel.components as List).indexOf(statusMessage)
                    parentPanel.remove(idx)
                    parentPanel.add(newPanel, idx)
                    parentPanel.validate()

// Cute groovyism, but we don't need the extra overhead.
//                parentPanel.with {
//                    //  We may not be the zeroth child anymore.
//                    def idx = (components as List).indexOf(statusMessage)
//                    remove(idx)
//                    add(newPanel, idx)
//                    validate()
//                }

//            myFrame.pack()

                    setStatus("Built")
                }
            }
        }

    }
}