package learn

// https://github.com/dialex/JColor
//@Grab(group='com.diogonunes', module='JColor', version='5.5.1')
import static com.diogonunes.jcolor.Ansi.*
import static com.diogonunes.jcolor.TextColorAttribute.*
import com.diogonunes.jcolor.Attribute

static void main(String[] args) {
    // Use Case 2: compose Attributes to create your desired format
    Attribute[] myFormat = new Attribute[]{RED_TEXT(), BLACK_BACK(), BOLD()};
    System.out.println(colorize("   This text will be red on black   ", myFormat));
    System.out.println("\n");
}
