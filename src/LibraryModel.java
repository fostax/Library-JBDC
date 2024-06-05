/*
 * LibraryModel.java
 * Author:
 * Created on:
 */


import javax.swing.*;
import java.sql.*;
import java.util.GregorianCalendar;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private final JFrame dialogParent;
    private Connection conn;


    public LibraryModel(JFrame parent, String userid, String password) {
        dialogParent = parent;
        String url = "jdbc:postgresql://depot.ecs.vuw.ac.nz:5432/" + userid + "_jdbc";
        try {
            Class.forName("org.postgresql.Driver");
            this.conn = DriverManager.getConnection(url, userid, password);
        } catch (SQLException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(dialogParent, "Error connecting to database\n Message: " + e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            closeDBConnection();
        }
    }

    public String bookLookup(int isbn) {
        StringBuilder result = new StringBuilder("Book Lookup:\n");
        String bookLookupSQL = "SELECT isbn, title, edition_no, numofcop, numleft, " +
                "STRING_AGG(surname, ', ' ORDER BY authorseqno) AS authors" +
                " FROM book" +
                " NATURAL JOIN book_author" +
                " NATURAL JOIN author" +
                " WHERE isbn = ?" +
                " GROUP BY isbn, title, edition_no, numofcop, numleft;";

        try (PreparedStatement bookLookupStmt = this.conn.prepareStatement(bookLookupSQL)) {
            bookLookupStmt.setInt(1, isbn);
            try (ResultSet bookLookupResult = bookLookupStmt.executeQuery()) {
                if (!bookLookupResult.next()) {
                    return result.append("\tNo such ISBN: ").append(isbn).toString();
                }
                do {
                    result.append("\t")
                            .append(bookLookupResult.getInt("isbn"))
                            .append(": ").append(bookLookupResult.getString("title"))
                            .append("\n")
                            .append("\t").append("Edition: ").append(bookLookupResult.getInt("edition_no"))
                            .append(" - ").append("Number of copies: ").append(bookLookupResult.getInt("numofcop"))
                            .append(" - ").append("Copies left: ").append(bookLookupResult.getInt("numleft"))
                            .append("\n")
                            .append("\t").append("Authors: ").append(bookLookupResult.getString("authors").trim());
                } while (bookLookupResult.next());
            }

        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\t").append(e.getMessage()).toString();
            }
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return result.toString();
    }

    public String showCatalogue() {
        StringBuilder result = new StringBuilder("Show Catalogue: \n");
        String showCatalogueSQL = "SELECT b.isbn, b.title, b.edition_no, b.numofcop, b.numleft, " +
                " STRING_AGG(a.surname, ', ' ORDER BY ba.authorseqno) AS authors," +
                " COUNT(DISTINCT a.authorid) AS num_authors" +
                " FROM book b" +
                " LEFT JOIN book_author ba ON b.isbn = ba.isbn" +
                " LEFT JOIN author a ON ba.authorid = a.authorid" +
                " GROUP BY b.isbn, b.title, b.edition_no, b.numofcop, b.numleft" +
                " ORDER BY b.isbn;";

        try (Statement showCatalogueStmt = this.conn.createStatement()) {
            try (ResultSet showCatalogueResult = showCatalogueStmt.executeQuery(showCatalogueSQL)) {
                if (!showCatalogueResult.next()) {
                    return result.append("\tCatalogue is Empty").toString();
                }
                do {
                    result.append("\n").append(showCatalogueResult.getInt("isbn")).append(": ")
                            .append(showCatalogueResult.getString("title").trim()).append("\n")
                            .append("\t").append("Edition: ").append(showCatalogueResult.getInt("edition_no"))
                            .append(" - Number of copies: ").append(showCatalogueResult.getInt("numofcop"))
                            .append(" - Copies left: ").append(showCatalogueResult.getInt("numleft")).append("\n")
                            .append("\t");

                    switch (showCatalogueResult.getInt("num_authors")) {
                        case 0 -> result.append("(no authors)");
                        case 1 -> result.append("Author: ").append(showCatalogueResult.getString("authors"));
                        default -> result.append("Authors: ").append(showCatalogueResult.getString("authors"));
                    }
                    result.append("\n");
                } while (showCatalogueResult.next());
            }

        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\t").append(e.getMessage()).toString();
            }
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return result.toString();
    }

    public String showLoanedBooks() {
        StringBuilder result = new StringBuilder("Show Loaned Books:\n");
        String getLoanedBooksSQL = "SELECT b.isbn, b.title, b.edition_no, b.numofcop, b.numleft, " +
                "STRING_AGG(a.surname, ', ' ORDER BY authorseqno) AS authors " +
                "FROM book b " +
                "LEFT JOIN book_author ba ON b.isbn = ba.isbn " +
                "LEFT JOIN author a ON ba.authorid = a.authorid " +
                "WHERE numleft < numofcop " +
                "GROUP BY b.isbn, b.title, b.edition_no, b.numofcop, b.numleft;";


        try (Statement getLoanedBooksStmt = this.conn.createStatement()) {
            try (ResultSet getLoanedBooksResult = getLoanedBooksStmt.executeQuery(getLoanedBooksSQL)) {
                if (!getLoanedBooksResult.next()) {
                    return result.append("\t(No Loaned Books)").toString();
                }
                do {
                    int book_isbn = getLoanedBooksResult.getInt("isbn");

                    result.append("\n").append(book_isbn).append(": ")
                            .append(getLoanedBooksResult.getString("title").strip()).append("\n")
                            .append("\tEdition: ").append(getLoanedBooksResult.getInt("edition_no"))
                            .append(" - ").append("Number of copies: ")
                            .append(getLoanedBooksResult.getInt("numofcop")).append(" - ")
                            .append("Copies left: ").append(getLoanedBooksResult.getInt("numleft")).append("\n")
                            .append("\tAuthors: ").append(getLoanedBooksResult.getString("authors")).append("\n")
                            .append("\tBorrowers:");

                    String getBorrowersSQL = "SELECT * FROM customer " +
                            "NATURAL JOIN cust_book cb WHERE isbn = ?";

                    try (PreparedStatement getBorrowersStmt = this.conn.prepareStatement(getBorrowersSQL)) {
                        getBorrowersStmt.setInt(1, book_isbn);
                        try (ResultSet getBorrowersResult = getBorrowersStmt.executeQuery()) {
                            while (getBorrowersResult.next()) {
                                result.append("\n\t\t").append(getBorrowersResult.getInt("customerid")).append(": ")
                                        .append(getBorrowersResult.getString("l_name").strip()).append(", ")
                                        .append(getBorrowersResult.getString("f_name").strip()).append(" - ")
                                        .append(getBorrowersResult.getString("city"));
                            }
                            result.append("\n");
                        }
                    }
                } while (getLoanedBooksResult.next());
            }


        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\t").append(e.getMessage()).toString();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return result.toString();
    }

    public String showAuthor(int authorID) {
        StringBuilder result = new StringBuilder("Show Author:\n");
        String showAuthorSQL = "SELECT a.authorid, a.name, a.surname, b.isbn, b.title, " +
                "(SELECT COUNT(*) FROM book_author ba2 WHERE ba2.authorid = a.authorid) AS num_books" +
                " FROM author a" +
                " LEFT JOIN book_author ba ON a.authorid = ba.authorid" +
                " LEFT JOIN book b ON ba.isbn = b.isbn" +
                " WHERE a.authorid = ?;";

        try (PreparedStatement bookLookupStmt = this.conn.prepareStatement(showAuthorSQL)) {
            bookLookupStmt.setInt(1, authorID);
            try (ResultSet bookLookupResult = bookLookupStmt.executeQuery()) {
                if (!bookLookupResult.next()) {
                    result.append("\t").append("No such author ID: ").append(authorID);
                } else {
                    boolean first = true;
                    do {
                        if (first) {
                            result.append("\t")
                                    .append(bookLookupResult.getInt("authorid")).append(" - ")
                                    .append(bookLookupResult.getString("name").trim()).append(" ")
                                    .append(bookLookupResult.getString("surname").trim())
                                    .append("\n");

                            if (bookLookupResult.getInt("num_books") == 1) {
                                result.append("\tBook written:\n");
                            } else if (bookLookupResult.getInt("num_books") > 1) {
                                result.append("\tBooks written:\n");
                            } else {
                                result.append("\tNo Books written.\n");
                                break;
                            }
                            first = false;
                        }
                        result.append("\t\t").append(bookLookupResult.getInt("isbn")).append(" - ")
                                .append(bookLookupResult.getString("title").trim())
                                .append("\n");
                    } while (bookLookupResult.next());
                }
            }
        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\t").append(e.getMessage()).toString();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return result.toString();
    }

    public String showAllAuthors() {
        StringBuilder result = new StringBuilder("Show All Authors\n");
        String allAuthorsSQL = "SELECT authorid, name, surname" +
                " FROM author" +
                " ORDER BY authorid;";
        try (PreparedStatement allAuthorsStmt = this.conn.prepareStatement(allAuthorsSQL)) {
            try (ResultSet allAuthorsResult = allAuthorsStmt.executeQuery()) {
                while (allAuthorsResult.next()) {
                    result.append("\t").append(allAuthorsResult.getInt("authorid")).append(": ")
                            .append(allAuthorsResult.getString("surname").trim())
                            .append(", ").append(allAuthorsResult.getString("name").trim()).append("\n");
                }
            }
        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\t").append(e.getMessage()).toString();
            }
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    public String showCustomer(int customerID) {
        StringBuilder result = new StringBuilder("Show Customer:\n");
        String showCustomerSQL = "SELECT c.customerid, c.f_name, c.l_name, c.city," +
                "(SELECT COUNT(*) FROM cust_book cb WHERE c.customerid = cb.customerid) AS num_books" +
                " FROM customer c WHERE customerid = ?";


        try (PreparedStatement showCustomerStmt = this.conn.prepareStatement(showCustomerSQL)) {
            showCustomerStmt.setInt(1, customerID);
            try (ResultSet showCustomerResult = showCustomerStmt.executeQuery()) {
                if (!showCustomerResult.next()) {
                    showCustomerResult.close();
                    return result.append("\tCustomer ").append(customerID).append(" does not exist").toString();
                } else {
                    result.append("\t").append(customerID).append(": ")
                            .append(showCustomerResult.getString("l_name").strip()).append(", ")
                            .append(showCustomerResult.getString("f_name").strip())
                            .append(" - ").append(showCustomerResult.getString("city")).append("\n");
                }

                if (showCustomerResult.getInt("num_books") < 1) {
                    return result.append("\t(No books borrowed)").toString();
                } else if (showCustomerResult.getInt("num_books") == 1) {
                    result.append("\tBook Borrowed:\n");
                } else {
                    result.append("\tBooks Borrowed:\n");
                }
            }

            String getBooksSQL = "SELECT isbn, title FROM cust_book NATURAL JOIN book WHERE customerid = ?";

            try (PreparedStatement getBooksStmt = this.conn.prepareStatement(getBooksSQL)) {
                getBooksStmt.setInt(1, customerID);
                try (ResultSet getBooksResult = getBooksStmt.executeQuery()) {
                    while (getBooksResult.next()) {
                        result.append("\t\t").append(getBooksResult.getInt("isbn")).append(" - ")
                                .append(getBooksResult.getString("title")).append("\n");
                    }
                }
            }

        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\t").append("Error fetching Books for customer : ").append(customerID)
                        .append(". Error: ").append(e.getMessage()).toString();
            }
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            e.printStackTrace();
            return null;
        }

        return result.toString();
    }

    public String showAllCustomers() {
        StringBuilder result = new StringBuilder("Show All Customers:\n");
        String allCustomersSQL = "SELECT * FROM customer;";

        try (Statement allCustomerStmt = this.conn.createStatement();
             ResultSet allCustomersResult = allCustomerStmt.executeQuery(allCustomersSQL)
        ) {
            if (!allCustomersResult.next()) {
                return result.append("\tNo Customers").toString();
            } else {
                do {
                    result.append("\t").append(allCustomersResult.getInt("customerid")).append(": ")
                            .append(allCustomersResult.getString("l_name").strip()).append(", ")
                            .append(allCustomersResult.getString("f_name").strip()).append(" - ");
                    if (allCustomersResult.getString("city") != null) {
                        result.append(allCustomersResult.getString("city"));
                    } else {
                        result.append("(no city)");
                    }
                    result.append("\n");
                } while (allCustomersResult.next());
            }
        } catch (SQLException e) {
            if (this.conn != null) {
                return result.append("\tError: ").append(e.getMessage()).toString();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return result.toString();
    }

    public String borrowBook(int isbn, int customerID,
                             int day, int month, int year) {
        StringBuilder result = new StringBuilder();
        String checkCustomerQuery = "SELECT * FROM customer WHERE customerid = ? FOR UPDATE";
        String bookNameSQL = "SELECT title FROM book WHERE isbn = ?";


        try (PreparedStatement checkCustomerStmt = this.conn.prepareStatement(checkCustomerQuery)) {
            this.conn.setAutoCommit(false);

            // Check if customer exists and lock record with FOR UPDATE
            checkCustomerStmt.setInt(1, customerID);
            try (ResultSet checkCustomerResult = checkCustomerStmt.executeQuery()) {
                if (!checkCustomerResult.next()) {
                    this.conn.rollback();
                    return result.append("\tNo such Customer ID: ").append(customerID).toString();
                }

                String f_name = checkCustomerResult.getString("f_name").strip();
                String l_name = checkCustomerResult.getString("l_name").strip();


                String lockBookQuery = "SELECT * FROM book WHERE isbn = ? AND numleft > 0 FOR UPDATE";
                try (PreparedStatement lockBookStmt = this.conn.prepareStatement(lockBookQuery)) {
                    // Check if book exists
                    lockBookStmt.setInt(1, isbn);
                    try (ResultSet lockBookResult = lockBookStmt.executeQuery()) {
                        if (!lockBookResult.next()) {
                            this.conn.rollback();
                            return result.append("\tBook not available: ").append(isbn).toString();
                        }
                    }
                }
                // Insert a new record into cust_book table
                Date dueDate = new Date(new GregorianCalendar(year, month, day).getTimeInMillis());
                String checkCustBookSQL = "SELECT * FROM cust_book WHERE customerid = ? AND isbn = ?";
                try (PreparedStatement checkCustBookStmt = this.conn.prepareStatement(checkCustBookSQL)) {
                    checkCustBookStmt.setInt(1, customerID);
                    checkCustBookStmt.setInt(2, isbn);

                    try (ResultSet checkCustBookResult = checkCustBookStmt.executeQuery()) {
                        if (checkCustBookResult.next()) {
                            this.conn.rollback();
                            return result.append("\tCustomer ").append(customerID).append(" already has Book ").append(isbn).toString();
                        }
                    }

                    String insertCustBookQuery = "INSERT INTO cust_book (isbn, duedate, customerid) VALUES (?, ?, ?)";
                    try (PreparedStatement insertCustBookStmt = this.conn.prepareStatement(insertCustBookQuery)) {
                        insertCustBookStmt.setInt(1, isbn);
                        insertCustBookStmt.setDate(2, dueDate);
                        insertCustBookStmt.setInt(3, customerID);
                        int numEntries = insertCustBookStmt.executeUpdate();
                        if (numEntries == 0) {
                            this.conn.rollback();
                            return result.append("\tError loaning book ").append(isbn).append(" to Customer ").append(customerID).toString();
                        }
                    }
                }

                // Interaction popup between steps 3 and 4
                JOptionPane.showMessageDialog(dialogParent, "Ready to update. Click OK to continue", "Tuple/s Locked", JOptionPane.INFORMATION_MESSAGE);

                // Update book table
                String updateBookQuery = "UPDATE book SET numleft = numleft - 1 WHERE isbn = ?";
                try (PreparedStatement updateBookStmt = this.conn.prepareStatement(updateBookQuery)) {
                    updateBookStmt.setInt(1, isbn);
                    int numEntries = updateBookStmt.executeUpdate();
                    if (numEntries == 0) {
                        this.conn.rollback();
                        return result.append("\tBook Update Failed: ").toString();
                    }
                }

                try (PreparedStatement bookNameStmt = this.conn.prepareStatement(bookNameSQL)) {
                    bookNameStmt.setInt(1, isbn);
                    try (ResultSet bookNameResult = bookNameStmt.executeQuery()) {
                        if (!bookNameResult.next()) {
                            this.conn.rollback();
                            return result.append("Failed retrieving title for ISBN: ").append(isbn).toString();
                        }
                        result.append("Borrow Book:\n")
                                .append("\tBook: ").append(isbn).append(" (")
                                .append(bookNameResult.getString("title").strip()).append(")\n")
                                .append("\tLoaned to: ").append(customerID)
                                .append(" (").append(f_name).append(" ").append(l_name).append(")\n")
                                .append("\tDue Date: ").append(dueDate);
                    }
                }
            }
            this.conn.commit();
        } catch (SQLException e) {
            try {
                this.conn.rollback();
            } catch (SQLException e2) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(dialogParent, "Rollback Failed: " + e.getMessage(), "Error", JOptionPane.INFORMATION_MESSAGE);
            }
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        } finally {
            if (this.conn != null) {
                try {
                    this.conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return result.toString();
    }

    public String returnBook(int isbn, int customerid) {
        StringBuilder result = new StringBuilder("Return Book:\n");
        String checkCustomerSQL = "SELECT * FROM customer NATURAL JOIN cust_book " +
                "WHERE customerid = ? AND isbn = ? FOR UPDATE";

        try (PreparedStatement checkCustomerStmt = this.conn.prepareStatement(checkCustomerSQL)) {
            this.conn.setAutoCommit(false);

            // Check if customer exists + has borrowed give book and lock record with FOR UPDATE
            checkCustomerStmt.setInt(1, customerid);
            checkCustomerStmt.setInt(2, isbn);
            try (ResultSet checkCustomerResult = checkCustomerStmt.executeQuery()) {
                if (!checkCustomerResult.next()) {
                    this.conn.rollback();
                    return result.append("\tBook ").append(isbn).append(" is not loaned to customer ").append(customerid).toString();
                }
            }

            // Check if book exists and lock it
            String lockBookSQL = "SELECT * FROM book WHERE isbn = ? FOR UPDATE";
            try (PreparedStatement lockBookStmt = this.conn.prepareStatement(lockBookSQL)) {
                lockBookStmt.setInt(1, isbn);
                try (ResultSet lockBookResult = lockBookStmt.executeQuery()) {
                    if (!lockBookResult.next()) {
                        this.conn.rollback();
                        return result.append("\tBook doesn't exist: ").append(isbn).toString();
                    }
                }
            }

            // Remove entry from cust_book
            String removeCustBookSQL = "DELETE FROM cust_book WHERE isbn = ? AND customerid = ?";
            try (PreparedStatement removeCustBookStmt = this.conn.prepareStatement(removeCustBookSQL)) {
                removeCustBookStmt.setInt(1, isbn);
                removeCustBookStmt.setInt(2, customerid);
                int numEntries = removeCustBookStmt.executeUpdate();

                if (numEntries == 0) {
                    this.conn.rollback();
                    return result.append("\tError removing book from cust_book table").toString();
                }
            }

            // Update book entry
            String updateBookSQL = "UPDATE book SET numleft = numleft + 1 WHERE isbn = ?";
            try (PreparedStatement updateBookStmt = this.conn.prepareStatement(updateBookSQL)) {
                updateBookStmt.setInt(1, isbn);
                int numEntries = updateBookStmt.executeUpdate();

                if (numEntries == 0) {
                    this.conn.rollback();
                    return result.append("\tFailed to Return Book ").append(isbn).append(" for Customer ").append(customerid).toString();
                } else {
                    result.append("\tBook ").append(isbn).append(" returned for customer ").append(customerid);
                }
            }

            this.conn.commit();
        } catch (SQLException e) {
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException e2) {
                    e2.printStackTrace();
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Error doing rollback", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
        } finally {
            if (conn != null) {
                try {
                    this.conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Error setting AutoCommit(true)", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        return result.toString();
    }

    public void closeDBConnection() {
        if (this.conn != null) {
            try {
                this.conn.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(dialogParent, "Could not close connection.\n Message: " + e.getMessage(), "Error Closing Connection", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    public String deleteCus(int customerID) {
        StringBuilder result = new StringBuilder("Delete Customer:\n");
        String checkCustomerSQL = "SELECT * FROM customer WHERE customerid = ?";


        try (PreparedStatement checkCustomerStmt = this.conn.prepareStatement(checkCustomerSQL)) {
            this.conn.setAutoCommit(false);

            checkCustomerStmt.setInt(1, customerID);
            try (ResultSet checkCustomerResult = checkCustomerStmt.executeQuery()) {
                if (!checkCustomerResult.next()) {
                    this.conn.rollback();
                    return result.append("\tCustomer ").append(customerID).append(" does not exist").toString();
                }
            }
            String checkCustBookSQL = "SELECT * FROM cust_book WHERE customerid = ?";
            try (PreparedStatement checkCustBookStmt = this.conn.prepareStatement(checkCustBookSQL)) {
                checkCustBookStmt.setInt(1, customerID);
                try (ResultSet checkCustBookResult = checkCustBookStmt.executeQuery()) {
                    if (checkCustBookResult.next()) {
                        do {
                            System.out.println(checkCustBookResult);
                            int custIsbn = checkCustBookResult.getInt("isbn");
                            String returned = returnBook(custIsbn, customerID);
                            result.append("\t\t").append(returned).append("\n");
                        } while (checkCustBookResult.next());
                    }
                }
            }

            // Delete Customer entry
            String deleteCustomerSQL = "DELETE FROM customer WHERE customerid = ?";
            try (PreparedStatement deleteCustomerStmt = this.conn.prepareStatement(deleteCustomerSQL)) {
                deleteCustomerStmt.setInt(1, customerID);
                int numCustomerEntries = deleteCustomerStmt.executeUpdate();
                if (numCustomerEntries == 0) {
                    this.conn.rollback();
                    return result.append("\tError deleting Customer ").append(customerID).toString();
                } else {
                    result.append("\tCustomer ").append(customerID).append(" Successfully Deleted.");
                }
            }

        } catch (SQLException e) {
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException e2) {
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Rollback Error", JOptionPane.INFORMATION_MESSAGE);
                }
                e.printStackTrace();
                return result.append("\tError Deleting Customer ").append(customerID).append(" - ").append(e.getMessage()).toString();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        } finally {
            if (this.conn != null) {
                try {
                    this.conn.setAutoCommit(true);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Error setting AutoCommit(true)", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        return result.toString();
    }

    public String deleteAuthor(int authorID) {
        StringBuilder result = new StringBuilder("Delete Author:\n");
        String checkAuthorSQL = "SELECT FROM author WHERE authorid = ? FOR UPDATE";

        try (PreparedStatement checkAuthorStmt = this.conn.prepareStatement(checkAuthorSQL)) {
            this.conn.setAutoCommit(false);

            checkAuthorStmt.setInt(1, authorID);
            try (ResultSet checkAuthorResult = checkAuthorStmt.executeQuery()) {
                if (!checkAuthorResult.next()) {
                    this.conn.rollback();
                    return result.append("\tNo Author matching Author ID: ").append(authorID).toString();
                }
            }

            String deleteBookAuthorSQL = "DELETE FROM book_author WHERE authorid = ?";
            try (PreparedStatement deleteBookAuthorStmt = this.conn.prepareStatement(deleteBookAuthorSQL)) {
                deleteBookAuthorStmt.setInt(1, authorID);
                int numEntries = deleteBookAuthorStmt.executeUpdate();
                if (numEntries > 0) {
                    result.append("\tAuthor ").append(authorID).append(" removed from ").append(numEntries);
                    result.append(numEntries == 1 ? (" book.\n") : (" books.\n"));
                }
            }

            String deleteAuthorSQL = "DELETE FROM author WHERE authorid = ? ";
            try (PreparedStatement deleteAuthorStmt = this.conn.prepareStatement(deleteAuthorSQL)) {
                deleteAuthorStmt.setInt(1, authorID);
                int numEntries = deleteAuthorStmt.executeUpdate();
                if (numEntries == 0) {
                    this.conn.rollback();
                    return result.append("\tError deleting Author ").append(authorID).toString();
                } else {
                    result.append("\tAuthor ").append(authorID).append(" deleted successfully");
                }
            }
        } catch (SQLException e) {
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException e2) {
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Rollback Error", JOptionPane.INFORMATION_MESSAGE);
                }
                return result.append("\tError Deleting Author: ").append(authorID).append(" ").append(e.getMessage()).toString();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        } finally {
            if (this.conn != null) {
                try {
                    this.conn.setAutoCommit(true);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Error setting AutoCommit(true)", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        return result.toString();
    }

    public String deleteBook(int isbn) {
        StringBuilder result = new StringBuilder("Delete Book:\n");
        String checkBookSQL = "SELECT isbn FROM book WHERE isbn = ? FOR UPDATE";
        String deleteCustBookSQL = "DELETE FROM cust_book WHERE isbn = ?";

        try (PreparedStatement checkBookStmt = this.conn.prepareStatement(checkBookSQL)) {
            this.conn.setAutoCommit(false);
            checkBookStmt.setInt(1, isbn);
            try (ResultSet checkBookResult = checkBookStmt.executeQuery()) {
                if (!checkBookResult.next()) {
                    this.conn.rollback();
                    return result.append("\tNo Book matching ISBN: ").append(isbn).toString();
                }
            }

            String checkCustBookSQL = "SELECT * FROM cust_book WHERE isbn = ? FOR UPDATE";
            try (PreparedStatement checkCustBookStmt = this.conn.prepareStatement(checkCustBookSQL)) {
                checkCustBookStmt.setInt(1, isbn);
                try (ResultSet checkCustBookResult = checkCustBookStmt.executeQuery()) {
                    if (checkCustBookResult.next()) {
                        try (PreparedStatement deleteCustBookStmt = this.conn.prepareStatement(deleteCustBookSQL)) {
                            deleteCustBookStmt.setInt(1, isbn);
                            int numEntries = deleteCustBookStmt.executeUpdate();
                            if (numEntries == 0) {
                                this.conn.rollback();
                                return result.append("\tError deleting book ").append(isbn).append(" from ")
                                        .append("cust_book table.").toString();
                            }
                        }
                    }
                }
            }

            String deleteBookAuthorSQL = "DELETE FROM book_author WHERE isbn = ?";
            try (PreparedStatement deleteBookAuthorStmt = this.conn.prepareStatement(deleteBookAuthorSQL)) {
                deleteBookAuthorStmt.setInt(1, isbn);
                deleteBookAuthorStmt.executeUpdate();
            }

            String deleteBookSQL = "DELETE FROM book WHERE isbn = ? ";
            try (PreparedStatement deleteBookStmt = this.conn.prepareStatement(deleteBookSQL)) {
                deleteBookStmt.setInt(1, isbn);
                int numEntries = deleteBookStmt.executeUpdate();
                if (numEntries == 0) {
                    return result.append("\tError deleting Book ").append(isbn).toString();
                } else {
                    result.append("\tBook ").append(isbn).append(" deleted successfully");
                }
            }

        } catch (SQLException e) {
            if (this.conn != null) {
                try {
                    this.conn.rollback();
                } catch (SQLException e2) {
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Rollback Error", JOptionPane.INFORMATION_MESSAGE);
                }
                return result.append("\tError Deleting Book: ").append(isbn).append(" ").append(e.getMessage()).toString();
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Connection Error", JOptionPane.INFORMATION_MESSAGE);
            return null;
        } finally {
            if (this.conn != null) {
                try {
                    this.conn.setAutoCommit(true);
                } catch (SQLException e) {
                    JOptionPane.showMessageDialog(dialogParent, e.getMessage(), "Error setting AutoCommit(true)", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }

        return result.toString();
    }

}