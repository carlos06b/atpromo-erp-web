package database;

import java.sql.Connection;

public class TestConnection {

    public static void main(String[] args) {

        try {

            Connection conn = ConnectionFactory.getConnection();

            if (conn != null) {
                System.out.println("Conectado com sucesso ao banco!");
            }

        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }

    }
}