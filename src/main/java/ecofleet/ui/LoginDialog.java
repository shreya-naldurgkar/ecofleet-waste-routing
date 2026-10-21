package ecofleet.ui;

import ecofleet.service.UserSession;
import ecofleet.service.GeneratorService;
import ecofleet.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.logging.Logger;

/**
 * Authentication dialog for ECOfleet.
 * Safe credential handling, clear error messages, and one-click demo role logins.
 */
public final class LoginDialog extends JDialog {
    private static final Logger LOGGER = Logger.getLogger(LoginDialog.class.getName());

    private final AppContext context;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;
    private final JButton loginButton;
    private final JProgressBar progressBar;

    private boolean authenticated = false;

    public LoginDialog(Frame parent, AppContext context) {
        super(parent, "ECOfleet — Sign In", true);
        this.context = context;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header Panel (Branding)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.BG_LIGHT);

        JLabel titleLabel = new JLabel("ECOfleet");
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        titleLabel.setForeground(UITheme.PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Waste Management & Routing System");
        subtitleLabel.setFont(UITheme.FONT_BODY);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(20));

        // Form Card Panel
        JPanel formCard = UITheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Username
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UITheme.FONT_BODY_BOLD);
        userLabel.setForeground(UITheme.TEXT_MAIN);
        formCard.add(userLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        usernameField = new JTextField(16);
        usernameField.setFont(UITheme.FONT_BODY);
        formCard.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UITheme.FONT_BODY_BOLD);
        passLabel.setForeground(UITheme.TEXT_MAIN);
        formCard.add(passLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        passwordField = new JPasswordField(16);
        passwordField.setFont(UITheme.FONT_BODY);
        formCard.add(passwordField, gbc);

        // Error message label
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.STATUS_DANGER_FG);
        formCard.add(errorLabel, gbc);

        // Progress bar
        gbc.gridy = 3;
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        formCard.add(progressBar, gbc);

        // Login Button
        gbc.gridy = 4;
        loginButton = UITheme.createPrimaryButton("Sign In");
        loginButton.addActionListener(e -> attemptLogin());
        formCard.add(loginButton, gbc);

        // Demo Logins Section
        JPanel demoPanel = new JPanel();
        demoPanel.setLayout(new BoxLayout(demoPanel, BoxLayout.Y_AXIS));
        demoPanel.setBackground(UITheme.BG_LIGHT);
        demoPanel.setBorder(new EmptyBorder(16, 0, 0, 0));

        JLabel demoHeader = new JLabel("Quick Demo Sign-In (Password: Password@123):");
        demoHeader.setFont(UITheme.FONT_SMALL);
        demoHeader.setForeground(UITheme.TEXT_MUTED);
        demoHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        demoPanel.add(demoHeader);
        demoPanel.add(Box.createVerticalStrut(8));

        JPanel demoButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        demoButtons.setBackground(UITheme.BG_LIGHT);

        demoButtons.add(createDemoButton("Admin", "admin", "Password@123"));
        demoButtons.add(createDemoButton("Operator", "operator1", "Password@123"));
        demoButtons.add(createDemoButton("Driver", "driver1", "Password@123"));
        demoButtons.add(createDemoButton("Hospital Demo", "gen_hospital", "Password@123"));

        demoPanel.add(demoButtons);

        JButton registerButton = new JButton("Create Generator Account");
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        registerButton.addActionListener(e -> createGeneratorAccount());
        demoPanel.add(Box.createVerticalStrut(8));
        demoPanel.add(registerButton);

        // Enter key submits form
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        };
        usernameField.addKeyListener(enterListener);
        passwordField.addKeyListener(enterListener);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formCard, BorderLayout.CENTER);
        mainPanel.add(demoPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }

    private JButton createDemoButton(String label, String user, String pass) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_SMALL);
        btn.setBackground(Color.WHITE);
        btn.setForeground(UITheme.PRIMARY);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER_COLOR, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        btn.addActionListener(e -> {
            usernameField.setText(user);
            passwordField.setText(pass);
            attemptLogin();
        });
        return btn;
    }

    private void createGeneratorAccount() {
        JTextField facilityName = new JTextField(18);
        JComboBox<String> generatorType = new JComboBox<>(new String[]{
                "Hospital", "Factory", "Housing Society", "School"});
        JTextField address = new JTextField(18);
        JComboBox<Zone> zone = new JComboBox<>();
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JTextField subtype = new JTextField(18);
        JTextField biomedicalAuth = new JTextField(18);
        try {
            List<Zone> zones = context.getCatalogService().registrationZones();
            for (Zone item : zones) zone.addItem(item);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not load zones: " + ex.getMessage(),
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (zone.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No service zones are configured. Ask an administrator to add a zone first.",
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel form = new JPanel(new GridLayout(8, 2, 8, 8));
        form.add(new JLabel("Organization / facility name:")); form.add(facilityName);
        form.add(new JLabel("Generator type:")); form.add(generatorType);
        form.add(new JLabel("Address:")); form.add(address);
        form.add(new JLabel("Service zone:")); form.add(zone);
        form.add(new JLabel("Login username:")); form.add(username);
        form.add(new JLabel("Password:")); form.add(password);
        form.add(new JLabel("License / registration number:")); form.add(subtype);
        form.add(new JLabel("Biomedical authorization (hospital only):")); form.add(biomedicalAuth);
        generatorType.addActionListener(e -> {
            boolean hospital = "Hospital".equals(generatorType.getSelectedItem());
            biomedicalAuth.setEnabled(hospital);
        });
        biomedicalAuth.setEnabled(false);

        int choice = JOptionPane.showConfirmDialog(this, form, "Create Generator Account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String name = facilityName.getText().trim();
        String type = switch ((String) generatorType.getSelectedItem()) {
            case "Hospital" -> "HOSPITAL";
            case "Factory" -> "FACTORY";
            case "Housing Society" -> "HOUSING_SOCIETY";
            default -> "SCHOOL";
        };
        String loginName = username.getText().trim();
        char[] secret = password.getPassword();
        Zone selectedZone = (Zone) zone.getSelectedItem();
        String subtypeValue = subtype.getText().trim();
        String biomedicalValue = biomedicalAuth.getText().trim();
        try {
            if (name.isBlank() || address.getText().trim().isBlank() || loginName.isBlank())
                throw new IllegalArgumentException("Facility name, address, and username are required.");
            Hospital hospital = null;
            HousingSociety society = null;
            Factory factory = null;
            switch (type) {
                case "HOSPITAL" -> {
                    if (subtypeValue.isBlank() || biomedicalValue.isBlank())
                        throw new IllegalArgumentException("Hospitals need a license and biomedical authorization number.");
                    hospital = new Hospital(null, subtypeValue, biomedicalValue);
                }
                case "HOUSING_SOCIETY" -> {
                    if (subtypeValue.isBlank()) throw new IllegalArgumentException("Enter the society registration number.");
                    society = new HousingSociety(null, subtypeValue);
                }
                case "FACTORY" -> {
                    if (subtypeValue.isBlank()) throw new IllegalArgumentException("Enter the factory pollution consent number.");
                    factory = new Factory(null, subtypeValue);
                }
                default -> { }
            }
            WasteGenerator generator = new WasteGenerator(null, name, type, null, null,
                    loginName + "@ecofleet.local", address.getText().trim(), null, null,
                    selectedZone.getZoneId(), true, LocalDateTime.now());
            long generatorId = context.getGeneratorService().registerGenerator(
                    new GeneratorService.Profile(generator, hospital, society, factory), loginName, secret);
            LOGGER.info("Generator account registered: generatorId=" + generatorId + ", type=" + type);
            UserSession session = context.getAuthService().login(loginName, secret);
            context.setCurrentSession(session);
            authenticated = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Account was not created: " + ex.getMessage(),
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            java.util.Arrays.fill(secret, '\0');
        }
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        if (username.isEmpty()) {
            errorLabel.setText("Please enter your username.");
            usernameField.requestFocusInWindow();
            return;
        }
        if (password.length == 0) {
            errorLabel.setText("Please enter your password.");
            passwordField.requestFocusInWindow();
            return;
        }

        loginButton.setEnabled(false);
        progressBar.setVisible(true);
        errorLabel.setText("Authenticating...");

        // Authenticate on background thread using SwingWorker
        SwingWorker<UserSession, Void> worker = new SwingWorker<>() {
            @Override
            protected UserSession doInBackground() {
                return context.getAuthService().login(username, password);
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                progressBar.setVisible(false);
                try {
                    UserSession session = get();
                    LOGGER.info(() -> "Authentication succeeded for role " + session.getRole());
                    context.setCurrentSession(session);
                    authenticated = true;
                    dispose();
                } catch (Exception ex) {
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    if (msg == null || msg.contains("Invalid credentials") || msg.contains("Credentials rejected")) {
                        errorLabel.setText("Invalid username or password.");
                    } else {
                        errorLabel.setText("Login failed: " + msg);
                    }
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                }
            }
        };
        worker.execute();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
