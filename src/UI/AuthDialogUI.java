package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

public class AuthDialogUI extends JDialog {
    private final String defaultDatabaseURL = "jdbc:postgresql://depot.ecs.vuw.ac.nz:5432/";
    private boolean okButtonClicked = false;
    private JPanel dialogPanel = new JPanel();
    private JPanel labelPanel = new JPanel();
    private JPanel inputPanel = new JPanel();
    private JTextField usernameTextField = new JTextField(20);
    private JPasswordField passwordTextField = new JPasswordField(20);
    private JTextField databaseURLField = new JTextField(defaultDatabaseURL);

    public AuthDialogUI() {
        this(null, "Authentication", false);
    }

    public AuthDialogUI(JFrame parent) {
        this(parent, "Authentication", true);
    }

    public AuthDialogUI(JFrame parent, String title) {
        this(parent, title, true);
    }

    public AuthDialogUI(final JFrame parent, String title, boolean modal) {
        super(parent, title, modal);

        // Set up close behaviour
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (!okButtonClicked)
                    System.exit(0);
            }
        });

        // Set up OK button behaviour
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (getUserName().isEmpty()) {
                    showMessageDialog(AuthDialogUI.this,
                            "Please enter a username",
                            "Format Error",
                            ERROR_MESSAGE);
                    return;
                }
                if (getDatabasePassword().isEmpty()) {
                    showMessageDialog(AuthDialogUI.this,
                            "Please enter a password",
                            "Format Error",
                            ERROR_MESSAGE);
                    return;
                }
                okButtonClicked = true;
                setVisible(false);
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        // Set up dialog contents
        labelPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 5, 5, 20));

        labelPanel.setLayout(new GridLayout(3, 1));
        labelPanel.add(new JLabel("User Name: "));
        labelPanel.add(new JLabel("Password:"));
        labelPanel.add(new JLabel("Database URL:"));
        inputPanel.setLayout(new GridLayout(3, 1));
        inputPanel.add(usernameTextField);
        inputPanel.add(passwordTextField);
        databaseURLField.addFocusListener(new FocusListener(){
            public void focusGained(FocusEvent e) {
                if (databaseURLField.getText().equals(defaultDatabaseURL)) {
                    databaseURLField.setText("");
                }
            }
            public void focusLost(FocusEvent e) {
                if (databaseURLField.getText().isEmpty()){
                    databaseURLField.setText(defaultDatabaseURL);
                }
            }
        });
        inputPanel.add(databaseURLField);

        Box buttonPane = new Box(X_AXIS);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(okButton);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(cancelButton);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        String introText = "Please enter your username and database password";
        JLabel introLabel = new JLabel(introText);
        introLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        getContentPane().add(introLabel, BorderLayout.NORTH);
        getContentPane().add(labelPanel, BorderLayout.WEST);
        getContentPane().add(inputPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPane, BorderLayout.SOUTH);

        // Ensure the enter key triggers the OK button
        getRootPane().setDefaultButton(okButton);

        // And that the escape key exits
        InputMap inputMap =
                getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getRootPane().getActionMap();
        inputMap.put(getKeyStroke("ESCAPE"), "exitAction");
        actionMap.put("exitAction", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        // Pack it all
        pack();

        // Center on the screen
        setLocationRelativeTo(null);
    }

    public String getUserName() {
        return usernameTextField.getText();
    }

    public String getDatabasePassword() {
        return new String(passwordTextField.getPassword());
    }

    public String getDatabaseURL() {
        return databaseURLField.getText();
    }
}
