package com.nepalnationalteam.blackjack;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JCheckBox chkShow = new JCheckBox("Show password");
    private final JCheckBox chkRemember = new JCheckBox("Remember me");
    private final FirebaseService firebase = new FirebaseService();

    public LoginFrame(StartScreenFrame start) {
        setTitle("Login");
        setSize(420, 360);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(new Color(20,20,20));
        setLayout(null);

        Color fg = new Color(220,220,220);

        JLabel lblEmail = new JLabel("Email");
        lblEmail.setForeground(fg);
        lblEmail.setBounds(40, 40, 120, 25);
        add(lblEmail);
        txtEmail.setBounds(40, 65, 330, 28);
        add(txtEmail);

        JLabel lblPass = new JLabel("Password");
        lblPass.setForeground(fg);
        lblPass.setBounds(40, 105, 120, 25);
        add(lblPass);
        txtPassword.setBounds(40, 130, 330, 28);
        txtPassword.setEchoChar('•');
        add(txtPassword);

        chkShow.setForeground(fg);
        chkShow.setOpaque(false);
        chkShow.setBounds(40, 165, 150, 20);
        add(chkShow);
        chkShow.addActionListener(e -> txtPassword.setEchoChar(chkShow.isSelected() ? (char)0 : '•'));

        chkRemember.setForeground(fg);
        chkRemember.setOpaque(false);
        chkRemember.setBounds(200, 165, 150, 20);
        add(chkRemember);

        JButton btnLogin = ui("Login");
        btnLogin.setBounds(40, 200, 150, 36);
        add(btnLogin);

        JButton btnBack = ui("Back");
        btnBack.setBounds(220, 200, 150, 36);
        add(btnBack);

        btnBack.addActionListener(e -> { dispose(); new StartScreenFrame().setVisible(true); });
        btnLogin.addActionListener(e -> doLogin());
    }

    private void doLogin() {
        String email = txtEmail.getText().trim();
        String pwd = new String(txtPassword.getPassword());
        if (email.isEmpty() || pwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email and password are required.");
            return;
        }
        try {
            FirebaseService.AuthResult r = firebase.signIn(email, pwd);
            SessionManager.Session s = new SessionManager.Session();
            s.email = r.email;
            s.displayName = r.displayName == null ? "" : r.displayName;
            s.idToken = r.idToken;
            s.refreshToken = r.refreshToken;
            s.localId = r.localId;
            s.rememberMe = chkRemember.isSelected();
            SessionManager.save(s);
            JOptionPane.showMessageDialog(this, "Welcome " + (s.displayName == null || s.displayName.isEmpty() ? s.email : s.displayName));
            dispose();
            new LobbyFrame(s.email, s.displayName).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login failed: " + ex.getMessage());
        }
    }

    private JButton ui(String t) {
        JButton b = new JButton(t);
        b.setBackground(new Color(30,30,30));
        b.setForeground(new Color(220,220,220));
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorder(BorderFactory.createLineBorder(new Color(70,70,70)));
        return b;
    }
}
