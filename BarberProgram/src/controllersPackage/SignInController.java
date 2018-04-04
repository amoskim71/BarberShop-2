package controllersPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONException;
import org.json.JSONObject;

/* Import javafx, java, mainPackage */
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import mainPackage.Connection;
import mainPackage.User;

public class SignInController implements Initializable {
	@FXML
	private Text actiontarget;
	@FXML
	private PasswordField passwordField;
	@FXML
	private TextField userField;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO - MySQL grabbing all user data should happen here and not each time a tab opens
		
		// DEBUG 
		userField.setText("raj@barbershop.com");
		passwordField.setText("barber");
	}

	@FXML
	protected void handleSubmitButtonAction(ActionEvent event) throws IOException {

		System.out.println("trying to connect to a php script...");

		String email = userField.getText().toString();
		String password = passwordField.getText().toString();

		System.out.println("We entered the values to send: " + email + ":" + password);

		String data = Connection.getInstance().URL_LOGIN + "?email=" + email + "&password=" + password;

		// send these values to the php script
		System.out.println("Connection: " + data);

		try {
			URL url = new URL(data);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			// Read the JSON output here
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;

			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// Try reading it in JSON format
			try {
				JSONObject json = new JSONObject(response.toString());
				System.out.println(json.getString("query_result"));

				String query_response = json.getString("query_result");

				if (query_response.equals("FAILED_LOGIN")) {
					// Give a response to the user that its incorrect
					System.out.println("Incorrect email or password entered!");
					actiontarget.setText("Wrong email or password!");
				} else if (query_response.equals("SUCCESSFUL_LOGIN")) {
					// We can go to the main program controller
					System.out.println("Successfull email and password entered!");
					
					// Read up the JSON values
					JSONObject obj = json.getJSONObject("0");

					//String id = obj.getString("id");
					User.getInstance().id = obj.getString("id");
					User.getInstance().created = obj.getString("created");
					User.getInstance().type = obj.getString("type");
					
					// Get profile data too
					loadProfileData();

					// Go to next stage
					login(event);
					
				} else {
					System.out.println("Not enough arguments were entered.. try filling both fields");
					actiontarget.setText("Enter in both fields!");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void loadProfileData(){
		// Get values from URL/JSON
		String data = Connection.getInstance().URL_GET_PROFILE + "?id=" + User.getInstance().id;
		
		// send these values to the php script
		System.out.println("Connecting to page ----------> " + data);

		try {
			URL url = new URL(data);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");

			// Read the JSON output here
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;

			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			// Try reading it in JSON format
			try {
				JSONObject json = new JSONObject(response.toString());
				System.out.println(json.getString("query_result"));

				String query_response = json.getString("query_result");

				if (query_response.equals("FAILED_PROFILE")) {
					// Give a response to the user that its incorrect
					System.out.println("Incorrect email or password entered!");
				} else if (query_response.equals("SUCCESSFUL_PROFILE")) {

					// Read up the JSON values
					JSONObject obj = json.getJSONObject("0");

					//String id = obj.getString("id");
					User.getInstance().first_name = obj.getString("first_name");
					User.getInstance().middle_name = obj.getString("middle_name");
					User.getInstance().last_name = obj.getString("last_name");
					User.getInstance().age = obj.getString("age");
					User.getInstance().home_telephone = obj.getString("home_telephone");
					User.getInstance().mobile = obj.getString("mobile");
					User.getInstance().emergency_name = obj.getString("emergency_name");
					User.getInstance().emergency_number = obj.getString("emergency_number");
					User.getInstance().profile_picture = obj.getString("profile_picture");
					
					
					// Set values for this logged in user
					
					
					
				} else {
					System.out.println("Not enough arguments were entered.. try filling both fields");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void login(ActionEvent event) throws IOException{
		Parent parent = FXMLLoader.load(getClass().getResource("/fxmlPackage/mainProgram.fxml"));
		Scene scene = new Scene(parent);
		Stage appStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
		appStage.setScene(scene);
		appStage.setTitle("Barber Shop");
		appStage.setWidth(944);
		appStage.setHeight(600);
		appStage.show();
	}

}
