
package com.nepalnationalteam.blackjackdesktop;

import javax.swing.*;
import java.awt.*;

public class RegisterFrame extends JFrame {
    private final JTextField txtEmail = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JPasswordField txtConfirm = new JPasswordField();
    private final JTextField txtDisplayName = new JTextField();
    private final JTextField txtId = new JTextField();

    private final JRadioButton rbId1 = new JRadioButton("Passport");
    private final JRadioButton rbId2 = new JRadioButton("Worker ID (NIE)");
    private final JRadioButton rbId3 = new JRadioButton("Normal ID (DNI)");

    private final JCheckBox chkShow = new JCheckBox("Show passwords");
    private final FirebaseService firebase = new FirebaseService();

    public RegisterFrame(StartScreenFrame start) {
        setTitle("Register");
        setSize(520, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setLayout(null);

        JLabel lblEmail = new JLabel("Email");
        lblEmail.setForeground(Color.WHITE);
        lblEmail.setBounds(50, 30, 120, 24);
        add(lblEmail);
        txtEmail.setBounds(50, 54, 360, 28);
        add(txtEmail);

        JLabel lblName = new JLabel("Display name");
        lblName.setForeground(Color.WHITE);
        lblName.setBounds(50, 90, 120, 24);
        add(lblName);
        txtDisplayName.setBounds(50, 114, 360, 28);
        add(txtDisplayName);

        JLabel lblPass = new JLabel("Password");
        lblPass.setForeground(Color.WHITE);
        lblPass.setBounds(50, 150, 120, 24);
        add(lblPass);
        txtPassword.setBounds(50, 174, 360, 28);
        txtPassword.setEchoChar('•');
        add(txtPassword);

        JLabel lblConf = new JLabel("Confirm password");
        lblConf.setForeground(Color.WHITE);
        lblConf.setBounds(50, 210, 150, 24);
        add(lblConf);
        txtConfirm.setBounds(50, 234, 360, 28);
        txtConfirm.setEchoChar('•');
        add(txtConfirm);

        chkShow.setBounds(50, 270, 200, 20);
        chkShow.setForeground(Color.WHITE);
        chkShow.setOpaque(false);
        add(chkShow);
        chkShow.addActionListener(e -> {
            char echo = chkShow.isSelected() ? (char)0 : '•';
            txtPassword.setEchoChar(echo);
            txtConfirm.setEchoChar(echo);
        });

        JLabel lblIds = new JLabel("Select ID");
        lblIds.setForeground(Color.WHITE);
        lblIds.setBounds(50, 300, 200, 20);
        add(lblIds);

        ButtonGroup group = new ButtonGroup();
        for (JRadioButton rb : new JRadioButton[]{rbId1, rbId2, rbId3}) {
            rb.setForeground(Color.WHITE);
            rb.setOpaque(false);
            group.add(rb);
            add(rb);
        }
        rbId1.setBounds(50, 325, 100, 20);
        rbId2.setBounds(150, 325, 100, 20);
        rbId3.setBounds(250, 325, 100, 20);
        JLabel lblIdVal = new JLabel("ID value");
        lblIdVal.setForeground(Color.WHITE);
        lblIdVal.setBounds(50, 350, 200, 20);
        add(lblIdVal);
        txtId.setBounds(50, 374, 360, 28);
        add(txtId);

        rbId1.setSelected(true);

        JButton btnCreate = uiButton("Create account");
        btnCreate.setBounds(50, 415, 160, 36);
        add(btnCreate);
        JButton btnBack = uiButton("Back");
        btnBack.setBounds(250, 415, 160, 36);
        add(btnBack);

        btnBack.addActionListener(e -> { dispose(); new StartScreenFrame().setVisible(true); });
        btnCreate.addActionListener(e -> doRegister());
    }

    private String getSelectedId() {
        if (rbId1.isSelected()) return "PASSPORT";
        if (rbId2.isSelected()) return "WORKER_NIE";
        return "NORMAL_DNI";
    }

    private void doRegister() {
        String email = txtEmail.getText().trim();
        String name = txtDisplayName.getText().trim();
        String p1 = new String(txtPassword.getPassword());
        String p2 = new String(txtConfirm.getPassword());
        String selectedId = getSelectedId();
        String idValue = txtId.getText().trim().toUpperCase();

        if (email.isEmpty() || name.isEmpty() || p1.isEmpty() || p2.isEmpty() || idValue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }
        if (!p1.equals(p2)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }
        
        // Validate ID according to selected type
        boolean ok;
        if ("PASSPORT".equals(selectedId)) {
            ok = validatePassport(idValue);
            if (!ok) { JOptionPane.showMessageDialog(this, "Invalid passport format. Use 6-9 alphanumeric characters."); return; }
        } else if ("WORKER_NIE".equals(selectedId)) {
            ok = validateNIE(idValue);
            if (!ok) { JOptionPane.showMessageDialog(this, "Invalid NIE. Expected X/Y/Z + 7 digits + correct control letter."); return; }
        } else { // NORMAL_DNI
            ok = validateDNI(idValue);
            if (!ok) { JOptionPane.showMessageDialog(this, "Invalid DNI. Expected 8 digits + correct control letter."); return; }
        }

        try {
            FirebaseService.AuthResult r = firebase.signUp(email, p1, name);

            if (FirebaseService.DATABASE_URL.startsWith("http")) {
                String userPath = FirebaseService.DATABASE_URL + "users/" + r.localId + ".json?auth=" + r.idToken;
                String body = "{\"email\":\"" + email.replace("\"","\\\"") + "\","
                            + "\"displayName\":\"" + name.replace("\"","\\\"") + "\","
                            + "\"selectedId\":\"" + selectedId + "\"}";
                java.net.URL url = new java.net.URL(userPath);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                byte[] out = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(out.length);
                conn.connect();
                try (java.io.OutputStream os = conn.getOutputStream()) { os.write(out); }
                int code = conn.getResponseCode();
                if (code >= 400) {
                    String resp = new String(conn.getErrorStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    throw new RuntimeException("Profile write failed: HTTP " + code + " " + resp);
                }
            }

            SessionManager.Session s = new SessionManager.Session();
            s.email = r.email;
            s.displayName = name;
            s.idToken = r.idToken;
            s.refreshToken = r.refreshToken;
            s.localId = r.localId;
            s.selectedId = selectedId;
            s.idType = selectedId;
            s.idValue = idValue;
            s.rememberMe = true;
            SessionManager.save(s);

            JOptionPane.showMessageDialog(this, "Account created. Welcome " + name + "!");
            dispose();
            new LobbyFrame(s.email, s.displayName).setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Register failed: " + ex.getMessage());
        }
    }

    
    private boolean validatePassport(String v) {
        if (v == null) return false;
        v = v.trim().toUpperCase();
        return v.matches("[A-Z0-9]{6,9}");
    }
    private boolean validateDNI(String v) {
        if (v == null) return false;
        v = v.trim().toUpperCase();
        if (!v.matches("\\\\d{8}[A-Z]")) return false;
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        int num = Integer.parseInt(v.substring(0,8));
        char expected = letters.charAt(num % 23);
        return v.charAt(8) == expected;
    }
    private boolean validateNIE(String v) {
        if (v == null) return false;
        v = v.trim().toUpperCase();
        if (!v.matches("[XYZ]\\\\d{7}[A-Z]")) return false;
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        char first = v.charAt(0);
        String numPart = v.substring(1,8);
        int prefix = (first=='X')?0:(first=='Y'?1:2);
        int num = Integer.parseInt(str(prefix) + numPart);
        char expected = letters.charAt(num % 23);
        return v.charAt(8) == expected;
    }
    private String str(int n){ return String.valueOf(n); }

    private JButton uiButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(30, 30, 30));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
        return btn;
    }
}
