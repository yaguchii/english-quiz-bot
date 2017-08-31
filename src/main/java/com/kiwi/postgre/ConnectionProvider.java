package com.kiwi.postgre;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

public class ConnectionProvider {
    public Connection getConnection() throws URISyntaxException,
            SQLException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String dbUrl = "jdbc:postgresql://"
                + dbUri.getHost()
                + ':'
                + dbUri.getPort()
                + dbUri.getPath()
                + "?sslmode=require&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";
        System.out.println(dbUrl);
        return DriverManager.getConnection(dbUrl, username, password);
    }
}