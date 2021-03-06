package controller;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import model.Connection;
import model.GUI;
import model.User;

public class AccountTabController extends ConnectionController implements Initializable {

	// FXML - GUI Components
	@FXML
	TextField tfFirstName;
	@FXML
	TextField tfMiddleName;
	@FXML
	TextField tfLastName;
	@FXML
	TextField tfAge;
	@FXML
	TextField tfHomeTel;
	@FXML
	TextField tfMobTel;
	@FXML
	TextField tfEmergencyName;
	@FXML
	TextField tfEmergencyTel;
	@FXML
	TextField tfEmail;
	@FXML
	TextField tfPassword;
	@FXML
	ImageView ivAccount;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Set GUI components
		updateGUI();
	}

	@FXML
	protected void handleUpdateAccount(ActionEvent event) throws IOException {

		String email = tfEmail.getText().toString();
		String password = tfPassword.getText().toString();

		// Validate that changes were made
		if (!email.equals(User.getInstance().email) || !password.equals(User.getInstance().password)) {
			System.out.println("Change in email or password found!");

			// We can now perform a validaition
			if (validAccount(email, password)) {
				// Send to php script
				updateAccount(email, password);
				updateGUI();
			}
		} else {
			GUI.createDialog("You didn't change your password or email", new String[] { "Ok" }, null);
		}

		// return outcome
		System.out.println("// End of Update Account");
	}

	/**
	 * Validates that we entered a correct email and password
	 * 
	 * @param email
	 * @param password
	 * @return true if data inputed was valid
	 */
	private boolean validAccount(String email, String password) {
		return true;
	}

	/**
	 * Sends data to a php script and updates the current user values
	 * 
	 * @param email
	 * @param password
	 */
	private boolean updateAccount(String email, String password) {
		boolean updated = false;
		String data = Connection.URL_UPDATE_ACCOUNT;

		try {
			// Build the parameters
			StringBuffer paramsBuilder = new StringBuffer();
			paramsBuilder.append("id=" + User.getInstance().id);
			paramsBuilder.append("&email=" + email);
			paramsBuilder.append("&password=" + password);
			
			// Post the parameters to page
			response = connectToPagePost(data, paramsBuilder);
			
			// See if we can update the account by parsing the response
			updated = parseJSONUpdateAccount(response, email, password);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Updated?: " + updated);
		return updated;
	}
	
	private boolean parseJSONUpdateAccount(StringBuffer response, String email, String password){
		// Read it in JSON
		try {
			makeJSON(response);
			
			if (query_response.equals("SUCCESSFUL_UPDATE_ACCOUNT")) {
				System.out.println("We can successfully delete this from the table!!!");
				GUI.createDialog("Account updated", new String[] { "Ok" }, null);
				// Update the current user
				User.getInstance().email = email;
				User.getInstance().password = password;
				return true;
			} else {
				System.out.println("Not enough arguments were entered.. try filling both fields");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return false;
	}

	@FXML
	protected void handleUpdatePicture(ActionEvent event) throws IOException {
		FileChooser fileChooser = new FileChooser();

		// Set extension filter
		FileChooser.ExtensionFilter extFilterJPG = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.JPG");
		FileChooser.ExtensionFilter extFilterPNG = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.PNG");
		fileChooser.getExtensionFilters().addAll(extFilterJPG, extFilterPNG);

		// Show open file dialog
		File file = fileChooser.showOpenDialog(null);

		// If we hit cancel, we didn't select any file
		if (file == null)
			return;

		System.out.println("File chosen is: " + file.getAbsolutePath());

		try {
			BufferedImage bufferedImage = ImageIO.read(file);
			Image image = SwingFXUtils.toFXImage(bufferedImage, null);
			ivAccount.setImage(image);

			// Upload to the database and change its location into users profile picture
			upload(file);

		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (NullPointerException ex) {
			ex.printStackTrace();
		}
	}

	private void upload(File file) {

	}

	@FXML
	protected void handleUpdateProfile(ActionEvent event) throws IOException {

		String[] val = new String[8];

		val[0] = tfFirstName.getText().toString();
		val[1] = tfMiddleName.getText().toString();
		val[2] = tfLastName.getText().toString();
		val[3] = tfAge.getText().toString();
		val[4] = tfHomeTel.getText().toString();
		val[5] = tfMobTel.getText().toString();
		val[6] = tfEmergencyName.getText().toString();
		val[7] = tfEmergencyTel.getText().toString();

		User usr = User.getInstance();
		usr.fillProfileData();

		boolean changed = false;
		for (int i = 0; i < val.length; i++) {
			if (!val[i].equals(usr.profileData[i])) {
				changed = true;
			}
		}

		// Validate that changes were made
		if (changed) {
			System.out.println("Change in profile was found!");

			// We can now perform a validation
			if (validProfile(val)) {
				// Send to php script
				updateProfile(val);
				updateGUI();
			}
		} else {
			GUI.createDialog("You didn't change any profile/emergency data", new String[] { "Ok" }, null);
		}

		// return outcome
		System.out.println("// End of Update Account");
	}

	private boolean updateProfile(String[] rgVals) throws IOException {
		// Assume we can't successfully update the page
		boolean updated = false;
		
		// Get the URL of the page we want to POST data to
		String data = Connection.URL_UPDATE_PROFILE;

		// Build up the parameters
		StringBuffer params = buildProfileParameters(rgVals);
		
		// Try to connect to the page with parameters
		response = connectToPagePost(data, params);
		
		// Check if we can update by parsing
		updated = parseJSONUpdateProfile(response, rgVals);
		
		// Returns true if successfully updated
		return updated;
	}
	
	/**
	 * Build a StringBuffer containing the parameters to send to a POST script
	 * @param rgVals
	 * @return
	 */
	private StringBuffer buildProfileParameters(String[] rgVals) {
		StringBuffer p = new StringBuffer();
		p.append("id=" + User.getInstance().id);
		p.append("&fName=" + rgVals[0]);
		p.append("&mName=" + rgVals[1]);
		p.append("&lName=" + rgVals[2]);
		p.append("&age=" + rgVals[3]);
		p.append("&tel=" + rgVals[4]);
		p.append("&mob=" + rgVals[5]);
		p.append("&eName=" + rgVals[6]);
		p.append("&eTel=" + rgVals[7]);
		return p;
	}
	
	private boolean parseJSONUpdateProfile(StringBuffer response, String[] rgVals) {
		
		boolean updated = false;
		
		// Read it in JSON
		try {
			makeJSON(response);
			
			
			if (query_response.equals("SUCCESSFUL_UPDATE_PROFILE")) {
				System.out.println("We can successfully delete this from the table!!!");
				updated = true;

				User usr = User.getInstance();

				// Update the current user
				usr.first_name = rgVals[0];
				usr.middle_name = rgVals[1];
				usr.last_name = rgVals[2];
				usr.age = rgVals[3];
				usr.home_telephone = rgVals[4];
				usr.mobile = rgVals[5];
				usr.emergency_name = rgVals[6];
				usr.emergency_number = rgVals[7];

				GUI.createDialog("Profile updated", new String[] { "Ok" }, null);
			} else {
				System.out.println("Not enough arguments were entered.. try filling both fields");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return updated;
	}

	private boolean validProfile(String[] rgVals) {
		return true;
	}

	private void updateGUI() {
		tfFirstName.setText(User.getInstance().first_name);
		tfMiddleName.setText(User.getInstance().middle_name);
		tfLastName.setText(User.getInstance().last_name);
		tfAge.setText(User.getInstance().age);
		tfHomeTel.setText(User.getInstance().home_telephone);
		tfMobTel.setText(User.getInstance().mobile);

		tfEmergencyName.setText(User.getInstance().emergency_name);
		tfEmergencyTel.setText(User.getInstance().emergency_number);

		tfEmail.setText(User.getInstance().email);
		tfPassword.setText(User.getInstance().password);
	}
}
