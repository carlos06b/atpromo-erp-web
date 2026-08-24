package controller;

import dao.UserDAO;
import model.User;

public class UserController {

    private UserDAO userDAO = new UserDAO();

    public void register(User user) {

        if (user.getName() == null || user.getName().isBlank()) {
            System.out.println("Nome inválido.");
            return;
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            System.out.println("Email inválido.");
            return;
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            System.out.println("Senha inválida.");
            return;
        }

        if (user.getJobTittle() == null || user.getJobTittle().isBlank()) {
            System.out.println("Cargo inválido.");
            return;
        }

        if (userDAO.findByEmail(user.getEmail()) != null) {
            System.out.println("Email já cadastrado.");
            return;
        }

        userDAO.save(user);
    }

    public User login(String email, String password) {

        User user = userDAO.login(email, password);

        if (user == null) {
            System.out.println("Email ou senha inválidos.");
            return null;
        }

        System.out.println("Login realizado com sucesso!");
        System.out.println("Usuário: " + user.getName());
        System.out.println("Cargo: " + user.getJobTittle());

        return user;
    }
}