import UI.LibraryUI;

import java.awt.*;

public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        // Build the UI on the Swing thread
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                new LibraryUI();
            }
        });
    }
}
