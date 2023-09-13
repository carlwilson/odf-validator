package org.openpreservation.odf.apps;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openpreservation.messages.Message;
import org.openpreservation.odf.pkg.OdfPackage;
import org.openpreservation.odf.pkg.OdfPackages;
import org.openpreservation.odf.pkg.ValidatingParser;
import org.openpreservation.odf.validation.ValidationReport;
import org.xml.sax.SAXException;

public class test {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        // Get a validating parser instance
        ValidatingParser packageParser = OdfPackages.getValidatingParser();

        File packageFile = new File("path/to/package.ods");

        // Get the OdfPackage instance from the parser
        OdfPackage odfPackage = packageParser.parsePackage(packageFile.toPath());

        // Now validate the package and get the validation report
        ValidationReport report = packageParser.validatePackage(odfPackage);

        // Is the package valid?
        if (report.isValid()) {
            System.out.println("Package is valid");
            // Get any warnings or info message (no errors as the package is valid)
            List<Message> messages = report.getMessages();
            // Loop through the messages
            for (Message message : messages) {
                // Get the message id
                System.out.println(message.getId());
                // Get the message severity (INFO, WARNING, ERROR)
                System.out.println(message.getSeverity());
                // Print out the message text
                System.out.println(message.getMessage());
            }
        } else {
            System.out.println("Package is not valid");
            // Get the error messages
            List<Message> messages = report.getErrors();
            for (Message message : messages) {
                // Get the message id
                System.out.println(message.getId());
                // Print out the message text
                System.out.println(message.getMessage());
            }
        }

    }

}
