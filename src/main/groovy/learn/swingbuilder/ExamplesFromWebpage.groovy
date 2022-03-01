package learn.swingbuilder

import groovy.beans.Bindable
import groovy.swing.SwingBuilder

import javax.swing.JFrame
import javax.swing.JSplitPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import java.awt.BorderLayout as BL

// https://web.archive.org/web/20140702234352/http://groovy.codehaus.org/Swing+Builder
class ExamplesFromWebpage {


    /**
     * simplest example
     */
    static void doIt_01() {
        def count = 0
        new SwingBuilder().edt {
            frame(title: 'Frame', size: [300, 300], show: true) {
                borderLayout()
                textlabel = label(text: "Click the button!", constraints: BL.NORTH)
                button(text: 'Click Me',
                        actionPerformed: { count++; textlabel.text = "Clicked ${count} time(s)."; println "clicked" },
                        constraints: BL.SOUTH)
            }
        }
    }


    /**
     * reuse a widget with state
     */
    static void doIt_02() {

        def swing = new SwingBuilder()

        def sharedPanel = {
            def count = 0
            swing.panel() {
                def textlabel = label("Click the button!")
                button(
                        text:'Click Me',
                        actionPerformed: {
                            count++
                            textlabel.text = "Clicked ${count} time(s)."
                            println "Clicked!"
                        }
                )
            }
        }


        swing.edt {
            frame(title:'Frame', defaultCloseOperation:JFrame.EXIT_ON_CLOSE, pack:true, show:true) {
                vbox {
                    widget(sharedPanel())
                    widget(sharedPanel())
                }
            }
        }
    }


    /**
     * connect two components to model, put actions in controller
     */

    static class MyModel {
        @Bindable int count = 0
    }


    /**
     * MVC Example
     */
    static void doIt_03() {

        // the model has observable properties
        def theModel = new MyModel()

        // the controller knows the model - but not the view.
        // the controller holds the logic to talk to change the model
        def theController = [
                countMinusThree: {theModel.count -= 3},
                countPlusFive: {theModel.count += 5}
        ]

        /**
         * VISUAL COMPONENT
         * this panel is decoupled by 
         * 1 bind (listen to model) and 
         * 2 action (action to model)
         * 3 swing parameter
         */
        def sharedPanel = {
            SwingBuilder mySwing, //be careful... closure closes over
            String sourceProperty, // will be bound
            Object source,  // will be bound
            Closure converter = {v->v?"clicks: $v":"not yet a value"},  // will be bound
            Closure doAction ->
            def count = 0
            mySwing.panel() {
                label(text: bind(
                        source: source,
                        sourceProperty: sourceProperty,
                        converter: converter))
                button(
                        text:'Click Me',
                        actionPerformed: {
                            doAction()
                            println "Clicked!"
                        }
                )
            }
        }


        /**
         * GLUE-CODE: layout, binding, transfroms and controller-actions age glued.
         */
        def swing = new SwingBuilder()
        swing.edt {
            frame(title:'Frame', defaultCloseOperation:JFrame.EXIT_ON_CLOSE, pack:true, show:true) {
                vbox {
                    label(text: "decoupled...")
                    widget(sharedPanel(swing, "count", theModel, {v->v?"= $v  (+5)":"zerrro"}, theController.countPlusFive))
                    widget(sharedPanel(swing, "count", theModel, theController.countMinusThree))
                }
            }
        }
    }

    static void doIt_04() {
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        def mboxes = [
                [name: "root@example.com", folders: [[name: "Inbox"], [name: "Trash"]]],
                [name: "test@foo.com", folders: [[name: "Inbox"], [name: "Trash"]]]
        ]
        def swing = new SwingBuilder()
        JTree mboxTree
        swing.frame(title: 'Mailer', defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
                size: [800, 600], show: true, locationRelativeTo: null) {
            //lookAndFeel("system")
            menuBar() {
                menu(text: "File", mnemonic: 'F') {
                    menuItem(text: "Exit", mnemonic: 'X', actionPerformed: {dispose() })
                }
            }
            splitPane {
                scrollPane(constraints: "left", preferredSize: [160, -1]) {
                    mboxTree = tree(rootVisible: false)
                }
                splitPane(orientation: JSplitPane.VERTICAL_SPLIT, dividerLocation:280) {
                    scrollPane(constraints: "top") { mailTable = table() }
                    scrollPane(constraints: "bottom") { textArea() }
                }
            }
            ["From", "Date", "Subject"].each { mailTable.model.addColumn(it) }
        }

        mboxTree.model.root.removeAllChildren()
        mboxes.each {mbox ->
            def node = new DefaultMutableTreeNode(mbox.name)
            mbox.folders.each { folder -> node.add(new DefaultMutableTreeNode(folder.name)) }
            mboxTree.model.root.add(node)
        }
        mboxTree.model.reload(mboxTree.model.root)

    }



    static void main(String[] args) {
        try {
            doIt_01()
            doIt_02()
            doIt_03()
            doIt_04()
        } catch (Exception e) {
            println "---------------------"
            println e.getMessage()
            println "---------------------"
            e.printStackTrace()
        }
    }

}




