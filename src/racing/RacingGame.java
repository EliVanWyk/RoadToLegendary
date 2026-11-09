package racing;

import javax.swing.SwingUtilities;

/**
 * Starts the program. Run THIS class in Eclipse (Run As > Java Application).
 */
public class RacingGame {

    public static void main(String[] args) {
        // Create the career (25 races, 5 ranks) and open the window.
        // The Driver is created after the player types a name on the title screen.
        final Career career = new Career();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameWindow window = new GameWindow(career);
                window.setVisible(true);
            }
        });
    }
}
