package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import org.json.JSONException;
import org.json.JSONObject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Connection;
import model.GUI;
import model.Service;
import model.User;

/**
 * Controller for the services tabbed page
 * 
 * @author Raj
 */
public class ServicesTabController extends ConnectionController implements Initializable {
	
	// FXML - GUI Components
	@FXML
	TableView<Service> serviceTable;
	@FXML
	TableColumn<Service, String> serviceID;
	@FXML
	TableColumn<Service, String> serviceName;
	@FXML
	TableColumn<Service, String> servicePrice;
	@FXML
	ChoiceBox<String> serviceChoiceBox;
	@FXML
	TextField tfPrice;
	@FXML
	Button btnRemoveService;
	
	// List of all known services this barber can have
	public static ArrayList<Service> myServices = new ArrayList<Service>();

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// Load up the table with the current users services
		int len = User.getInstance().services.size();

		// Connect each column with a service and its value
		serviceID.setCellValueFactory(new PropertyValueFactory<Service, String>("Id"));
		serviceName.setCellValueFactory(new PropertyValueFactory<Service, String>("Service"));
		servicePrice.setCellValueFactory(new PropertyValueFactory<Service, String>("Price"));

		// We can do something with the table now since we have services!
		if (len > 0) {
			serviceTable.getItems().setAll(User.getInstance().services);
		}

		List<String> cities = User.getInstance().getServices();
		ObservableList<String> list = FXCollections.observableArrayList(cities);
		serviceChoiceBox.setItems(list);
	}

	

	@FXML
	protected void handleRemoveService(ActionEvent event) throws IOException {

		Service selectedItems = serviceTable.getSelectionModel().getSelectedItems().get(0);

		if (selectedItems == null) {
			GUI.createDialog("Select something first", new String[] { "Ok" }, null);
		} else {

			// Create the functions that will be called on the two buttons in
			// this dialog we're gunna create
			Callable<Void>[] functions = new Callable[2];

			// Only create a function for the 'yes' button as no will cancel
			functions[0] = new Callable<Void>() {
				public Void call() {
					try {
						Service selectedItems = serviceTable.getSelectionModel().getSelectedItems().get(0);
						String removeService = selectedItems.toString();
						System.out.println(removeService);

						// Send this value to a php script
						if (deleteService(selectedItems.getId())) {
							// Remove from Users services Array
							removeFromUsers(selectedItems);
							// After we get confirmation from the script, remove
							// from
							// GUI
							updateGUI();
						}
					} catch (NullPointerException e) {
						System.err.println("Select something first.");
					}
					return null;
				}
			};

			// Create the dialog with the passed function calls
			GUI.createDialog("Are you sure you want to delete this service?", new String[] { "Yes", "No" }, functions);
		}
	}

	/**
	 * Removes a selected service from the table view from the current User
	 * services arraylist
	 * 
	 * @param selectedItems
	 *            the Service to remove
	 */
	private void removeFromUsers(Service selectedItems) {
		// If we find a match, remove it from the List
		for (int i = 0; i < User.getInstance().services.size(); i++) {
			if (User.getInstance().services.get(i).getService().equals(selectedItems.getService())) {
				// Remove this from the users
				User.getInstance().services.remove(i);
			}
		}
	}

	/**
	 * Updates/refreshes the GUI of any changes
	 */
	private void updateGUI() {
		serviceTable.getItems().clear();
		serviceTable.getItems().addAll(User.getInstance().services);
	}

	/**
	 * Deletes a service from the MySQL database
	 * 
	 * @param id
	 *            the id of the service we want to delete
	 * @return true if a successful delete occurred
	 */
	private boolean deleteService(int id) {
		boolean deleted = false;
		String data = Connection.URL_DELETE_SERVICE;

		try {
			// Build parameters
			StringBuffer paramsBuilder = new StringBuffer();
			paramsBuilder.append("id=" + id);

			// Post them to web page
			response = connectToPagePost(data, paramsBuilder);
			deleted = parseJSONDeleteService();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return deleted;
	}
	
	private boolean parseJSONDeleteService() {
		// Read it in JSON
		try {
			makeJSON(response);

			if (query_response.equals("SUCCESSFUL_DELETE_SERVICE")) {
				System.out.println("We can successfully delete this from the table!!!");
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
	protected void handleSubmitService(ActionEvent event) throws IOException {

		boolean valid = true;

		// Validate that data was selected/entered (service name, price)
		if (serviceChoiceBox.getValue() == null || tfPrice.getText().equals("")) {
			valid = false;
			GUI.createDialog("Either service or price is not entered!", new String[] { "Ok" }, null);
		} else {
			// Both have been filled

			// Check if this service already exists in the table
			for (int i = 0; i < User.getInstance().services.size(); i++) {
				if (serviceChoiceBox.getValue().equals(User.getInstance().services.get(i).getService())) {
					System.out.println("This service already exists! ");
					GUI.createDialog("This service already exists!", new String[] { "Ok" }, null);
					return;
				}
			}

			// Check if we can format the price
			try {
				double price = Double.parseDouble(tfPrice.getText());
			} catch (NumberFormatException e) {
				GUI.createDialog("Incorrect price entered!", new String[] { "Ok" }, null);
				return;
			}
		}

		if (valid) {

			String service = serviceChoiceBox.getValue();
			String price = tfPrice.getText();

			// Change it on the database via script, get the ID value back then
			// update the GUI!
			String lastID = insertService(service, price);

			if (!lastID.equals("-1")) {
				String[] args = new String[3];
				args[0] = lastID; // ID value would be an increment from the
									// last id (on mysql database)
				args[1] = service;
				args[2] = price;
				// Validate that we can enter this into the table
				User.getInstance().services.add(new Service(args));

				updateGUI();
			} else {
				// TODO - Display error message to the user that we couldn't
				// insert onto the database
			}
		}

		System.out.println("Can we enter in this table? " + valid);
	}

	/**
	 * Sends data to a php script that will attempt to INSERT to a MySQL
	 * database
	 * 
	 * @param service
	 *            the name of the service
	 * @param price
	 *            the price of the service with decimals
	 * @return the last service id entered onto the database
	 */
	private String insertService(String service, String price) {
		// Assume we couldn't insert this
		String retStr = "-1";

		// Send values to a php script, get the json value back, return the json
		// value
		String data = Connection.URL_INSERT_SERVICE;

		try {
			data += "?barber_id=" + User.getInstance().id;
			data += "&service=" + URLEncoder.encode(service, "UTF-8");
			data += "&price=" + price;
			data += "&shop_id=" + User.getInstance().shop_id;
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		// Try connecting to the php script and passing the values above to it
		try {
			response = connectToPage(data);

			retStr = parseJSONInsertService(response, retStr);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return retStr;
	}
	
	private String parseJSONInsertService(StringBuffer response, String retStr) {
		// Try reading it in JSON format
		try {
			JSONObject json = new JSONObject(response.toString());
			System.out.println(json.getString("query_result"));
			String query_response = json.getString("query_result");

			if (query_response.equals("FAILED_INSERT_SERVICE")) {
				// Give a response to the user that its incorrect
				retStr = "-1";
			} else if (query_response.equals("SUCCESSFUL_INSERT_SERVICE")) {
				retStr = Integer.toString(json.getInt("last_id"));
			} else {
				System.out.println("Not enough arguments were entered.. try filling both fields");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return retStr;
	}
}