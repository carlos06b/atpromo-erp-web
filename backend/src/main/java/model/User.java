package model;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String jobTittle;

    public User(int id, String email, String jobTittle, String name, String password) {
        this.id = id;
        this.email = email;
        this.jobTittle = jobTittle;
        this.name = name;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getJobTittle() {
        return jobTittle;
    }

    public void setJobTittle(String jobTittle) {
        this.jobTittle = jobTittle;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
