/*
 * LibraryModel.java
 * Author:
 * Created on:
 */


import javax.swing.*;
import java.sql.*;

public class LibraryModel {

    // For use in creating dialogs and making them modal
    private JFrame dialogParent;
    private Connection conn;


    public LibraryModel(JFrame parent, String userid, String password) {
        dialogParent = parent;
        String url = "jdbc:postgresql://depot.ecs.vuw.ac.nz:5432/" + userid + "_jdbc";
        try {
            Class.forName("org.postgresql.Driver");
            this.conn = DriverManager.getConnection(url, userid, password);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage()); //TODO make a popup
        }
    }

    public String bookLookup(int isbn) {
        StringBuilder result = new StringBuilder("Book Lookup:\n");
        try {
            String stmt = "SELECT isbn, title, edition_no, numofcop, numleft, " +
                    "STRING_AGG(surname, ', ' ORDER BY authorseqno) AS authors" +
                    " FROM book" +
                    " NATURAL JOIN book_author" +
                    " NATURAL JOIN author" +
                    " WHERE isbn = ?" +
                    " GROUP BY isbn, title, edition_no, numofcop, numleft;";

            PreparedStatement ps = this.conn.prepareStatement(stmt);
            ps.setInt(1, isbn);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                result.append("\t").append("No such ISBN: ").append(isbn);
            } else {
                do {
                    result.append("\t")
                            .append(rs.getInt("isbn"))
                            .append(": ").append(rs.getString("title"))
                            .append("\n")
                            .append("\t").append("Edition: ").append(rs.getInt("edition_no"))
                            .append(" - ").append("Number of copies: ").append(rs.getInt("numofcop"))
                            .append(" - ").append("Copies left: ").append(rs.getInt("numleft"))
                            .append("\n")
                            .append("\t").append("Authors: ").append(rs.getString("authors"));
                } while (rs.next());
            }
            ps.close();
            rs.close();
        } catch (SQLException sqlE) {
            System.out.println(sqlE.getMessage()); //TODO make a popup
        }
        return result.toString();
    }

    public String showCatalogue() {
        StringBuilder result = new StringBuilder("Show Catalogue: \n");
        try {
            String stmt = "SELECT b.isbn, b.title, b.edition_no, b.numofcop, b.numleft, " +
                    " STRING_AGG(a.surname, ', ' ORDER BY ba.authorseqno) AS authors," +
                    " COUNT(DISTINCT a.authorid) AS num_authors" +
                    " FROM book b" +
                    " LEFT JOIN book_author ba ON b.isbn = ba.isbn" +
                    " LEFT JOIN author a ON ba.authorid = a.authorid" +
                    " GROUP BY b.isbn, b.title, b.edition_no, b.numofcop, b.numleft" +
                    " ORDER BY b.isbn;";
            Statement s = this.conn.createStatement();
            ResultSet rs = s.executeQuery(stmt);

            while (rs.next()) {
                result.append("\n").append(rs.getInt("isbn")).append(": ")
                        .append(rs.getString("title")).append("\n")
                        .append("\t").append("Edition: ").append(rs.getInt("edition_no"))
                        .append(" - Number of copies: ").append(rs.getInt("numofcop"))
                        .append(" - Copies left: ").append(rs.getInt("numleft")).append("\n")
                        .append("\t");

                switch (rs.getInt("num_authors")) {
                    case 0 -> result.append("(no authors)");
                    case 1 -> result.append("Author: ").append(rs.getString("authors"));
                    default -> result.append("Authors: ").append(rs.getString("authors"));
                }
                result.append("\n");
            }
        } catch (SQLException sqlE) {
            System.out.println(sqlE.getMessage()); //TODO make a popup
        }

        return result.toString();
    }

    public String showLoanedBooks() {
        return "Show Loaned Books Stub";
    }

    public String showAuthor(int authorID) {
        return "Show Author Stub";
    }

    public String showAllAuthors() {
        return "Show All Authors Stub";
    }

    public String showCustomer(int customerID) {
        return "Show Customer Stub";
    }

    public String showAllCustomers() {
        return "Show All Customers Stub";
    }

    public String borrowBook(int isbn, int customerID,
                             int day, int month, int year) {
        return "Borrow Book Stub";
    }

    public String returnBook(int isbn, int customerid) {
        return "Return Book Stub";
    }

    public void closeDBConnection() {
    }

    public String deleteCus(int customerID) {
        return "Delete Customer";
    }

    public String deleteAuthor(int authorID) {
        return "Delete Author";
    }

    public String deleteBook(int isbn) {
        return "Delete Book";
    }
}