package pdf

import gui.models.AbstractGridLoadModel
import gui.models.GridLoadElement
import org.jfree.pdf.PDFDocument
import org.jfree.pdf.PDFGraphics2D
import org.jfree.pdf.PDFHints
import org.jfree.pdf.Page

import javax.swing.*
import java.awt.*

class PDFExport {
    static void saveToFile(JComponent c, String pathname, String title) {
        try {
            //JComponent c = (JComponent) panel.getContentPane().getComponent(0);
            PDFDocument pdfDoc = new PDFDocument();
            pdfDoc.setTitle(title);
            pdfDoc.setAuthor("v-pipe export");
            Page page = pdfDoc.createPage(new Rectangle(c.getWidth(), c.getHeight()));
            PDFGraphics2D g2 = page.getGraphics2D();
            g2.setRenderingHint(PDFHints.KEY_DRAW_STRING_TYPE,
                    PDFHints.VALUE_DRAW_STRING_TYPE_VECTOR);
            c.paint(g2);
            File f = new File(pathname);
            pdfDoc.writeToFile(f);
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    static void saveToFileData(AbstractGridLoadModel model, String pathname, String title) {
        try {
            int count = 1
            String result = "keep-together, department, week, load, yellow, red, need-from-task, task-project, task-start, task-end, task-total-capa, task-info\n"
            for (x in 0 .. model.sizeX-1) {
                for (y in 0 .. model.sizeY-1) {
                    GridLoadElement element = model.getElement(x, y)
                    for (d in element.projectDetails) {
                        result += count + ", "
                        result += element.department + ", "
                        result += element.timeString + ", "
                        result += element.load + ", "
                        result += element.yellow + ", "
                        result += element.red + ", "
                        result += d.projectCapaNeed + ", "
                        result += d.originalTask.toCSV() + "\n"
                    }
                    count++
                }
            }
            File f = new File(pathname);
            f.write(result)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
