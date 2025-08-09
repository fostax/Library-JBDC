package UI;
/**
 * UI.LibraryUI.java former Comp302.java
 * <p>
 * Created on March 6, 2003, 2:52 PM with netbeans
 * <p>
 * Updated on March 25 2005 by Jerome Dolman
 */
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Month;
import java.util.Calendar;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.awt.event.KeyEvent.VK_E;
import static java.awt.event.KeyEvent.VK_T;
import static java.util.Calendar.*;
import static javax.swing.BoxLayout.X_AXIS;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.*;
import static javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
import static javax.swing.KeyStroke.getKeyStroke;
import Model.LibraryModel;

public class LibraryUI extends JFrame {
    // Actions
    private Action exitAction;
    private Action clearTextAction;
    private Action borrowAction;
    private Action returnAction;

    // The main output area
    private JTextArea outputArea;

    // Return fields
    private JTextField returnISBN;
    private JTextField returnCustomerID;

    // Borrow fields
    private JTextField borrowISBN;
    private JTextField borrowCustomerID;
    private JComboBox borrowDay;
    private JComboBox borrowMonth;
    private JComboBox borrowYear;

    // Buttons and tabbed pane - keep them for focus order
    private JButton returnButton;
    private JButton borrowButton;
    private JTabbedPane tabbedPane;

    // The data model
    private LibraryModel model;

    // A parent for modal dialogs
    private JFrame dialogParent = this;

    /**
     * Create a new UI.LibraryUI object - showing an authentication dialog
     * then bringing up the main window.
     */
    public LibraryUI() {
        super("JDBC Library");
        // Uncomment the following if you'd rather not see everything in bold
        //UIManager.put("swing.boldMetal", Boolean.FALSE);

        // Initialise everything
        initActions();
        initUI();
        initFocusTraversalPolicy();
        setSize(600, 600);

        // Show Authentication dialog
        AuthDialogUI ad = new AuthDialogUI(this, "Authentication");
        ad.setVisible(true);
        String username = ad.getUserName();
        String password = ad.getDatabasePassword();
        String databaseURL = ad.getDatabaseURL();

        // Create data model
        model = new LibraryModel(this, username, password, databaseURL);

        // Center window on screen
        GraphicsEnvironment ge =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        Point center = ge.getCenterPoint();
        setLocation(center.x - getSize().width / 2,
                center.y - getSize().height / 2);

        // Show ourselves
        setVisible(true);
    }

    // Convenience method for the LookupAction constructor
    private static boolean isVowel(char c) {
        switch (Character.toLowerCase(c)) {
            case 'a':
            case 'e':
            case 'i':
            case 'o':
            case 'u':
                return true;
            default:
                return false;
        }
    }

    private void initActions() {
        exitAction = new ExitAction();
        clearTextAction = new ClearTextAction();
        borrowAction = new BorrowAction();
        returnAction = new ReturnAction();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                doExit();
            }
        });
    }

    private void initUI() {
        // Create tabbed pane with commands in it
        tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane, BorderLayout.NORTH);

        tabbedPane.addTab("Book", null, createBookPane(),
                "View book information");
        tabbedPane.addTab("Author", null, createAuthorPane(),
                "View author information");
        tabbedPane.addTab("Customer", null, createCustomerPane(),
                "View customer information");
        tabbedPane.addTab("Borrow Book", null, createBorrowPane(),
                "Borrow books for a customer");
        tabbedPane.addTab("Return Book", null, createReturnPane(),
                "Return books for a customer");

        // Create output area with scrollpane
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFocusable(false);
        outputArea.setTabSize(2);
        JScrollPane sp = new JScrollPane(outputArea);
        sp.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

        getContentPane().add(sp, BorderLayout.CENTER);

        // Create menus
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');

        JMenuItem clearTextMenuItem = new JMenuItem(clearTextAction);
        JMenuItem exitMenuItem = new JMenuItem(exitAction);

        fileMenu.add(clearTextMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Pack it all
        pack();
    }

    // By default the GridBagLayout stuffs up tab ordering for the
    // borrow book and return book panes, so I need this to ensure it's all
    // the right way round.
    private void initFocusTraversalPolicy() {
        Container nearestRoot =
                (isFocusCycleRoot()) ? this : getFocusCycleRootAncestor();
        final FocusTraversalPolicy defaultPolicy =
                nearestRoot.getFocusTraversalPolicy();

        MapFocusTraversalPolicy mine =
                new MapFocusTraversalPolicy(defaultPolicy, tabbedPane);
        mine.putAfter(returnISBN, returnCustomerID);
        mine.putAfter(returnCustomerID, returnButton);
        mine.putAfter(returnButton, tabbedPane);
        mine.putAfter(borrowISBN, borrowCustomerID);
        mine.putAfter(borrowCustomerID, borrowDay);
        mine.putAfter(borrowDay, borrowMonth);
        mine.putAfter(borrowMonth, borrowYear);
        mine.putAfter(borrowYear, borrowButton);
        mine.putAfter(borrowButton, tabbedPane);

        mine.putBefore(returnCustomerID, returnISBN);
        mine.putBefore(returnButton, returnCustomerID);
        mine.putBefore(borrowCustomerID, borrowISBN);
        mine.putBefore(borrowDay, borrowCustomerID);
        mine.putBefore(borrowMonth, borrowDay);
        mine.putBefore(borrowYear, borrowMonth);
        mine.putBefore(borrowButton, borrowYear);

        mine.putTabBefore("Borrow Book", borrowButton);
        mine.putTabBefore("Return Book", returnButton);

        nearestRoot.setFocusTraversalPolicy(mine);
    }

    private Container createBookPane() {
        // Create buttons
        JButton bookLookup = new JButton(new BookLookupAction());
        JButton showCat = new JButton(new ShowCatalogueAction());
        JButton showLoanedBook = new JButton(new ShowLoanedBooksAction());
        JButton deleteBook = new JButton(new DeleteBookAction());

        // Create panel
        Box pane = new Box(X_AXIS);
        pane.add(Box.createHorizontalGlue());
        pane.add(bookLookup);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(showCat);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(showLoanedBook);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(deleteBook);
        pane.add(Box.createHorizontalGlue());
        return pane;
    }

    private Container createAuthorPane() {
        // Create buttons
        JButton showAuthor = new JButton(new ShowAuthorAction());
        JButton showAllAuth = new JButton(new ShowAllAuthorsAction());
        JButton deleteAuthor = new JButton(new DeleteAuthorAction());
        // Create panel
        Box pane = new Box(X_AXIS);
        pane.add(Box.createHorizontalGlue());
        pane.add(showAuthor);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(showAllAuth);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(deleteAuthor);
        pane.add(Box.createHorizontalGlue());

        return pane;
    }

    private Container createCustomerPane() {
        // Create buttons
        JButton showCus = new JButton(new ShowCustomerAction());
        JButton showAllCus = new JButton(new ShowAllCustomersAction());
        JButton deleteCus = new JButton(new DeleteCustomerAction());

        // Create panel
        Box pane = new Box(X_AXIS);
        pane.add(Box.createHorizontalGlue());
        pane.add(showCus);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(showAllCus);
        pane.add(Box.createHorizontalStrut(5));
        pane.add(deleteCus);
        pane.add(Box.createHorizontalGlue());

        return pane;
    }

    private Container createBorrowPane() {
        // Initialise date combo boxes
        borrowDay = new JComboBox();
        borrowMonth = new JComboBox();
        borrowYear = new JComboBox();

        String[] months = Stream.of(Month.values())
                .map(Month::name)
                .map(String::toLowerCase)
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .toArray(String[]::new);

        // Get all years from 20 years ago to now
        Calendar today = Calendar.getInstance();
        int todayYear = today.get(YEAR);
        String[] years = IntStream.rangeClosed(todayYear - 20, todayYear)
                        .mapToObj(String::valueOf)
                        .toArray(String[]::new);

        borrowMonth.setModel(new DefaultComboBoxModel(months));
        borrowYear.setModel(new DefaultComboBoxModel(years));

        // Set initial selection to today
        int todayDay = today.get(DAY_OF_MONTH);
        int todayMonth = today.get(MONTH);

        // Set initial selections before adding listeners
        borrowMonth.setSelectedIndex(todayMonth);
        borrowYear.setSelectedItem(String.valueOf(todayYear));

        // Add listeners to update days based on month and year
        ActionListener updateDaysListener = e -> updateDaysComboBox();
        borrowMonth.addActionListener(updateDaysListener);
        borrowYear.addActionListener(updateDaysListener);

        // Initialize days for current month and year
        updateDaysComboBox();

        // Set selected day after updateDaysComboBox() called.
        borrowDay.setSelectedItem(String.valueOf(todayDay));

        // Create borrow button
        borrowButton = new JButton(borrowAction);

        // Create text fields
        borrowISBN = new JTextField(15);
        borrowCustomerID = new JTextField(15);

        // Create panel and layout
        JPanel pane = new JPanel();
        pane.setOpaque(false);
        GridBagLayout gb = new GridBagLayout();
        pane.setLayout(gb);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 5, 1, 5);

        // Fill panel
        c.anchor = GridBagConstraints.EAST;
        addToGridBag(gb, c, pane, new JLabel("ISBN:"), 0, 0, 1, 1);
        addToGridBag(gb, c, pane, new JLabel("Customer ID:"), 0, 1, 1, 1);
        addToGridBag(gb, c, pane, new JLabel("Due Date:"), 0, 2, 1, 1);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        addToGridBag(gb, c, pane, borrowISBN, 1, 0, 3, 1);
        addToGridBag(gb, c, pane, borrowCustomerID, 1, 1, 3, 1);

        c.fill = GridBagConstraints.NONE;
        addToGridBag(gb, c, pane, borrowDay, 1, 2, 1, 1);
        addToGridBag(gb, c, pane, borrowMonth, 2, 2, 1, 1);
        addToGridBag(gb, c, pane, borrowYear, 3, 2, 1, 1);

        addToGridBag(gb, c, pane, borrowButton, 4, 0, 1, 3);

        // Set up VK_ENTER triggering the borrow button in this panel
        InputMap input = pane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        input.put(getKeyStroke("ENTER"), "borrowAction");
        pane.getActionMap().put("borrowAction", borrowAction);

        return pane;
    }

    private void updateDaysComboBox() {
        int selectedMonth = borrowMonth.getSelectedIndex();
        String yearString = (String) borrowYear.getSelectedItem();

        if (yearString == null) {
            return;
        }

        int selectedYear = Integer.parseInt(yearString);

        // Calculate days in selected month/year
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Store current selection
        String currentDay = (String) borrowDay.getSelectedItem();

        // Update days combo box
        String[] days = IntStream.rangeClosed(1, daysInMonth)
                .mapToObj(String::valueOf)
                .toArray(String[]::new);
        borrowDay.setModel(new DefaultComboBoxModel<>(days));

        // Restore selection if still valid
        if ( currentDay != null && Integer.parseInt(currentDay) <= daysInMonth) {
            borrowDay.setSelectedItem(currentDay);
        }
    }

    private Container createReturnPane() {
        // Create return button
        returnButton = new JButton(returnAction);

        // Create text fields
        returnISBN = new JTextField(15);
        returnCustomerID = new JTextField(15);

        // Create panel and layout
        JPanel pane = new JPanel();
        pane.setOpaque(false);
        GridBagLayout gb = new GridBagLayout();
        pane.setLayout(gb);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(1, 5, 1, 5);

        // Fill panel
        c.anchor = GridBagConstraints.EAST;
        addToGridBag(gb, c, pane, new JLabel("ISBN:"), 0, 0, 1, 1);
        addToGridBag(gb, c, pane, new JLabel("Customer ID:"), 0, 1, 1, 1);

        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        addToGridBag(gb, c, pane, returnISBN, 1, 0, 3, 1);
        addToGridBag(gb, c, pane, returnCustomerID, 1, 1, 3, 1);

        c.fill = GridBagConstraints.NONE;
        addToGridBag(gb, c, pane, returnButton, 4, 0, 1, 3);

        // Set up VK_ENTER triggering the return button in this panel
        InputMap input = pane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        input.put(getKeyStroke("ENTER"), "returnAction");
        pane.getActionMap().put("returnAction", returnAction);

        return pane;
    }

    private void addToGridBag(GridBagLayout gb, GridBagConstraints c,
                              Container cont, JComponent item,
                              int x, int y, int w, int h) {
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = w;
        c.gridheight = h;
        gb.setConstraints(item, c);
        cont.add(item);
    }

    private void appendOutput(String str) {
        if (str != null && !str.equals(""))
            outputArea.append(str + "\n\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void showExceptionDialog(Exception e) {
        showMessageDialog(this,
                e.toString(),
                "Error performing action",
                ERROR_MESSAGE);
    }

    /** Exit the Application */
    private void doExit() {
        model.closeDBConnection();
        System.exit(0);
    }

    private class ExitAction extends AbstractAction {
        public ExitAction() {
            super("Exit");
            putValue(MNEMONIC_KEY, VK_E);
            putValue(ACCELERATOR_KEY, getKeyStroke("ctrl Q"));
        }

        public void actionPerformed(ActionEvent evt) {
            doExit();
        }
    }

    private class ClearTextAction extends AbstractAction {
        public ClearTextAction() {
            super("Clear Text");
            putValue(MNEMONIC_KEY, VK_T);
            putValue(ACCELERATOR_KEY, getKeyStroke("ctrl T"));
        }

        public void actionPerformed(ActionEvent evt) {
            Document document = outputArea.getDocument();
            try {
                document.remove(0, document.getLength());
            } catch (BadLocationException ble) {
            }
        }
    }

    /**
     * An Action that catches any exception thrown in the doAction method.
     */
    private abstract class CatchAction extends AbstractAction {
        public CatchAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                doAction();
            } catch (Exception ex) {
                showExceptionDialog(ex);
            }
        }

        /** Subclasses implement this for their behaviour */
        protected abstract void doAction();
    }

    private class ReturnAction extends CatchAction {
        public ReturnAction() {
            super("Return");
        }

        public void doAction() {
            try {
                int isbn = Integer.parseInt(returnISBN.getText());
                int cusID = Integer.parseInt(returnCustomerID.getText());
                appendOutput(model.returnBook(isbn, cusID));
            } catch (NumberFormatException nfe) {
                showMessageDialog(dialogParent, "The values entered for ISBN or customer ID do not have number format. Please try again.",
                        "Format Error", ERROR_MESSAGE);
            }
        }
    }

    private class BorrowAction extends CatchAction {
        public BorrowAction() {
            super("Borrow");
        }

        public void doAction() {
            try {
                int isbn = Integer.parseInt(borrowISBN.getText());
                int cusID = Integer.parseInt(borrowCustomerID.getText());
                int day = Integer.parseInt((String) borrowDay.getSelectedItem());
                int year = Integer.parseInt((String) borrowYear.getSelectedItem());
                int month = borrowMonth.getSelectedIndex();
                appendOutput(model.borrowBook(isbn, cusID, day, month, year));
            } catch (NumberFormatException nfe) {
                showMessageDialog(dialogParent,
                        "The values entered for ISBN or customer ID do not have a numeric format. Please try again.",
                        "Format Error", ERROR_MESSAGE);
            }
        }
    }

    /**
     * A base class for lookup-based actions: prompt for a number with
     * various strings in the right places and call the doLookup method.
     */
    private abstract class LookupAction extends CatchAction {
        String title, itemDesc, a;

        public LookupAction(String name, String itemDesc) {
            this(name, itemDesc, isVowel(itemDesc.charAt(0)) ? "an" : "a");
        }

        public LookupAction(String name, String itemDesc, String a) {
            super(name);
            title = name;
            this.itemDesc = itemDesc;
            this.a = a;
        }

        protected void doAction() {
            try {
                Object in = showInputDialog(dialogParent,
                        "Enter " + a + " " + itemDesc,
                        title,
                        QUESTION_MESSAGE,
                        null, null, null);
                if (in == null)
                    return;
                int item = Integer.parseInt((String) in);
                doLookup(item);
            } catch (NumberFormatException nfe) {
                String message =
                        "The " + itemDesc + " entered does not have a numeric " +
                                "format.  Please try again.";
                showMessageDialog(dialogParent, message,
                        "Format Error", ERROR_MESSAGE);
            }
        }

        /** Subclasses implement this for their behaviour */
        protected abstract void doLookup(int id);
    }

    private class ShowCustomerAction extends LookupAction {
        public ShowCustomerAction() {
            super("Show Customer", "customer ID");
        }

        protected void doLookup(int customerID) {
            appendOutput(model.showCustomer(customerID));
        }
    }

    private class ShowAuthorAction extends LookupAction {
        public ShowAuthorAction() {
            super("Show Author", "author ID");
        }

        protected void doLookup(int authorID) {
            appendOutput(model.showAuthor(authorID));
        }
    }

    private class BookLookupAction extends LookupAction {
        public BookLookupAction() {
            super("Book Lookup", "ISBN");
        }

        protected void doLookup(int isbn) {
            appendOutput(model.bookLookup(isbn));
        }
    }

    private class DeleteCustomerAction extends LookupAction {
        public DeleteCustomerAction() {
            super("Delete Customer", "customer ID");
        }

        protected void doLookup(int customerID) {
            appendOutput(model.deleteCus(customerID));
        }
    }

    private class DeleteAuthorAction extends LookupAction {
        public DeleteAuthorAction() {
            super("Delete Author", "author ID");
        }

        protected void doLookup(int authorID) {
            appendOutput(model.deleteAuthor(authorID));
        }
    }

    private class DeleteBookAction extends LookupAction {
        public DeleteBookAction() {
            super("Delete Book", "ISBN");
        }

        protected void doLookup(int isbn) {
            appendOutput(model.deleteBook(isbn));
        }
    }

    private class ShowAllCustomersAction extends CatchAction {
        public ShowAllCustomersAction() {
            super("Show All Customers");
        }

        protected void doAction() {
            appendOutput(model.showAllCustomers());
        }
    }

    private class ShowAllAuthorsAction extends CatchAction {
        public ShowAllAuthorsAction() {
            super("Show All Authors");
        }

        protected void doAction() {
            appendOutput(model.showAllAuthors());
        }
    }

    private class ShowCatalogueAction extends CatchAction {
        public ShowCatalogueAction() {
            super("Show Catalogue");
        }

        protected void doAction() {
            appendOutput(model.showCatalogue());
        }
    }

    private class ShowLoanedBooksAction extends CatchAction {
        public ShowLoanedBooksAction() {
            super("Show Loaned Books");
        }

        protected void doAction() {
            appendOutput(model.showLoanedBooks());
        }
    }
}