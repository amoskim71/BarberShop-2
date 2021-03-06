package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import model.Booking;
import model.BookingCell;
import model.BusinessDay;
import model.Connection;
import model.User;

public class AppointTabController extends ConnectionController implements Initializable {

	// FXML - GUI Components
	@FXML
	TableView<ObservableList<BookingCell>> appointTable;
	@FXML
	DatePicker appointDate;

	// Constants
	private final String BOOKING_COLOURS = "src/colours.csv";

	// Table data
	private List<String> columns = new ArrayList<String>();
	private List<String> rows = new ArrayList<String>();
	private List<List<BookingCell>> rowsData = new ArrayList<List<BookingCell>>(7);

	private ObservableList<ObservableList<BookingCell>> csvData = FXCollections.observableArrayList();

	private ArrayList<String> closedDays = new ArrayList<String>();
	// Custom row colour names
	private ArrayList<String> tableNames = new ArrayList<String>();
	private ArrayList<String> tableColours = new ArrayList<String>();

	private ArrayList<Booking> currentBookings = new ArrayList<Booking>();

	// Whenever we click on a date on the date picker we store the days of the week
	// in date form here
	private ArrayList<String> currentDates = new ArrayList<String>();

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

	private String getDay(int day) {
		String[] days = { "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday" };
		return days[day];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		appointDate.setShowWeekNumbers(true);
		appointDate.setValue(NOW_LOCAL_DATE());

		loadCellColours();

		load();

		appointTable.setFixedCellSize(25);
		appointTable.prefHeightProperty()
				.bind(Bindings.size(appointTable.getItems()).multiply(appointTable.getFixedCellSize()).add(30));

		// END
		System.out.println("// END of AppointTab Initialize");
	}

	private void loadData() {

		// Clear everything before rebuilding the table
		csvData.clear();
		columns.clear();
		rows.clear();
		appointTable.getColumns().clear();
		appointTable.getItems().clear();

		// Build the table columns and headers from the json output
		loadTableColumns();

		// Load up the current bookings from date selected
		loadTableRows();

		// Creates the columns and rows visually from the two arraylists columns + rows
		createTable();
	}

	private void loadCellColours() {
		// Read the csv file
		File f = new File(BOOKING_COLOURS);
		if (f.exists() && !f.isDirectory()) {
			try (FileReader fin = new FileReader(f); BufferedReader in = new BufferedReader(fin);) {
				String l;
				while ((l = in.readLine()) != null) {
					// Break up the csv (should be two values e.g:
					// Booking, light green
					// brokenLine[0] = Booking
					// brokenLine[1] = light green
					String[] brokenLine = l.split(",");

					// Save to the String ArrayLists to use when colouring cells
					tableNames.add(brokenLine[0]);
					tableColours.add(brokenLine[1]);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * We know there is going to be 7 rows (Monday-Sunday) We already know how many
	 * columns there are (columns array) Now we need to create the cell data so that
	 * it matches the Dates and Time to each cell If X is number of time intervals
	 * then total cell data must be 7x + 7(days)
	 */
	private void loadTableRows() {
		rowsData.clear();
		// 7 rows
		for (int i = 0; i < 7; i++) {
			String day = getDay(i);
			String date = getDate(i);
			List<BookingCell> weekData = new ArrayList<BookingCell>();

			// First value in row is going to be Day
			weekData.add(new BookingCell(new String[] { "00:00:00", date, day, day }, false, null));

			// Next X values are times
			for (int z = 1; z < columns.size(); z++) {
				// Create BookingCell and place it into ObservableData
				String time = columns.get(z);

				// re factor this
				Booking b = booked(time, date);
				String text = "Open";
				if (b != null)
					text = "Booked";

				// Check if its a closed day today
				for (int p = 0; p < closedDays.size(); p++) {
					if (day.equals(closedDays.get(p))) {
						text = "Closed";
					}
				}

				weekData.add(new BookingCell(new String[] { time, date, day, text }, true, b));

			}
			rowsData.add(weekData);
		}
	}

	private String getDate(int i) {
		return currentDates.get(i);
	}

	// Checks whether or not there is a booking on this date
	private Booking booked(String time, String date) {

		// quick hack, not neat!
		// Converts 09:00:00 to 09:00 the b.getStartTime() format
		time = time.substring(0, time.length() - 3);

		// Check if there's a matching booking
		for (int i = 0; i < currentBookings.size(); i++) {
			Booking b = currentBookings.get(i);
			if (b.getDate().equals(date) && b.getStartTime().equals(time)) {
				// Its a matching date
				return b;
			}
		}

		return null;
	}

	private void createTable() {

		buildTableColumns();
		buildTableRows();

		appointTable.setItems(csvData);

		System.out.println("CSV SIZE: " + csvData.size());
		System.out.println("ROW SIZE: " + csvData.get(6).size());
		System.out.println(appointTable.getItems().size());
	}

	private void buildTableColumns() {
		for (int i = 0; i < columns.size(); i++) {
			final int finalIdx = i;

			// re factor substring here
			String name = columns.get(i);
			if (name.length() > 3) {
				name = name.substring(0, name.length() - 3);
			}

			TableColumn<ObservableList<BookingCell>, BookingCell> column = new TableColumn<>(name);
			// Set the text of the cells
			setupColumnNames(column, finalIdx);
			setupColumnDesign(column);

			// Add the columns to the table
			appointTable.getColumns().add(column);
		}
	}

	private void setupColumnNames(TableColumn<ObservableList<BookingCell>, BookingCell> column, int finalIdx) {
		column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().get(finalIdx)));
	}

	private void setupColumnDesign(TableColumn<ObservableList<BookingCell>, BookingCell> column) {
		// Set the colour of the cell
		column.setCellFactory(param -> {
			return new TableCell<ObservableList<BookingCell>, BookingCell>() {
				protected void updateItem(BookingCell item, boolean empty) {
					super.updateItem(item, empty);

					super.setText(empty ? "" : getItem().getText());
					super.setGraphic(null);

					if (item == null || empty) {
						super.setStyle("");
					} else if (updateTableItem(item)) {
						super.setStyle("-fx-background-color:grey");
						super.setText("-");
					} else {
						colourCells(item);
					}
				}

				private void colourCells(BookingCell item) {
					// Setting the colours based of the csv file
					for (int i = 0; i < tableNames.size(); i++) {
						if (item.getText().equals(tableNames.get(i))) {
							super.setStyle("-fx-background-color:" + tableColours.get(i));
						}
					}
				}
			};
		});
	}

	private boolean updateTableItem(BookingCell item) {
		boolean valid = false;

		try {
			// Check that this is not a Day column
			if (item.getUseable()) {
				if (new SimpleDateFormat("dd/MM/yy HH:mm:ss").parse(item.getDate() + " " + item.getTime())
						.before(new Date())) {

					if (item.getText().equals("Open")) {
						valid = true;
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return valid;
	}

	private void buildTableRows() {
		for (int i = 0; i < rowsData.size(); i++) {

			ObservableList<BookingCell> row = FXCollections.observableArrayList();

			// Loop through columns (1 removed due to empty column name)
			for (int c = 0; c < columns.size(); c++) {

				BookingCell b = rowsData.get(i).get(c);
				row.add(b);

				System.out.println(b.getDay() + " " + b.getDate() + " " + b.getTime() + " booked? " + b.getBooking());

			}

			csvData.add(row);
		}
	}

	private void loadTableColumns() {
		try {
			response = connectToPage(Connection.URL_BUSINESS_HOURS);
			// Parse our Column names
			parseBusinessHoursColumnNames(response);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BusinessDay[] loadBusinessHours(BusinessDay[] rgBHours) throws JSONException {
		for (int i = 0; i < 7; i++) {
			JSONObject animal = json.getJSONObject(Integer.toString(i));
			int day = animal.getInt("DayOfWeek");
			String open = animal.getString("OpenTime");
			String close = animal.getString("CloseTime");
			String interval = animal.getString("Interval");

			rgBHours[i] = new BusinessDay(day, new String[] { open, close, interval });
			System.out.println(
					"On " + getDay(day) + " the opening hours are " + open + " and closing hours are " + close);
		}
		return rgBHours;
	}

	private void parseBusinessHoursColumnNames(StringBuffer response) {
		// Try reading it in JSON format
		try {
			json = new JSONObject(response.toString());
			query_response = json.getString("query_business_hours");

			if (query_response.equals("SUCCESS_HOURS")) {

				// 7 Days, Monday-Sunday
				BusinessDay[] rgBHours = new BusinessDay[7];
				// Work out the business hours of a week
				rgBHours = loadBusinessHours(rgBHours);
				rgBHours = loadStartEndTime(rgBHours);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private BusinessDay[] loadStartEndTime(BusinessDay[] rgBHours) throws ParseException {
		// Now calculate the earliest opening hour and the latest
		// closing hour so we can see how many columns
		// we need to make
		String startTime = "23:30:00";
		String closeTime = "00:00:00";
		int interval = 0;

		Date start = simpleDateFormat.parse(startTime);
		Date end = simpleDateFormat.parse(closeTime);
		for (int i = 0; i < 7; i++) {

			BusinessDay b = rgBHours[i];

			// firstly check if open and closing are the same, in
			// which case we can just ignore
			// assume same times means its a closed day
			if (!b.getOpeningHours().equals(b.getClosingHours())) {

				Date checkingOpenTime = simpleDateFormat.parse(b.getOpeningHours());
				Date checkingCloseTime = simpleDateFormat.parse(b.getClosingHours());
				// Compare dates
				if (checkingOpenTime.before(start)) {
					start = checkingOpenTime;
				}

				if (checkingCloseTime.after(end)) {
					end = checkingCloseTime;
				}

				// Check if a higher interval was found
				if (b.getInterval() > interval) {
					interval = b.getInterval();
				}

			} else {
				System.out.println("It must be closed today on a " + b.getDay());
				closedDays.add(b.getDay());
			}
		}

		loadColumns(new Date[] { start, end }, interval);

		return rgBHours;
	}

	private void loadColumns(Date[] dates, int interval) {
		long cols = ((Math.abs((dates[0].getTime() - dates[1].getTime())) / 1000) / 60) / interval;

		columns.clear();
		// Empty first column
		columns.add("");
		for (int i = 0; i <= cols; i++) {
			columns.add(simpleDateFormat.format(dates[0]));
			dates[0] = addMinutesToDate(interval, dates[0]);
		}
	}

	private static Date addMinutesToDate(int minutes, Date beforeTime) {
		final long ONE_MINUTE_IN_MILLIS = 60000;// millisecs

		long curTimeInMs = beforeTime.getTime();
		Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
		return afterAddingMins;
	}

	@FXML
	protected void handleDateButtonAction(ActionEvent event) throws IOException {
		// Get the week number of a date selected
		load();
	}

	public void load() {
		LocalDate date = appointDate.getValue();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String getDate = date.format(formatter);

		storeDates(getDate);

		// Get the current dates bookings
		getBookings(getDate);

		// Build the table and match based off the bookings
		loadData();

		System.out.println(currentBookings.toString());
	}

	/**
	 * Populates the current dates ArrayList
	 * 
	 * @param getDate
	 */
	private void storeDates(String getDate) {

		// Reset
		currentDates.clear();

		LocalDate date = appointDate.getValue();

		// Calculate
		Calendar now = Calendar.getInstance();
		now.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth() - 1);
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
		String[] days = new String[7];
		int delta = -now.get(GregorianCalendar.DAY_OF_WEEK) + 2;
		// add 2 if your week start on monday
		now.add(Calendar.DAY_OF_MONTH, delta);
		for (int i = 0; i < 7; i++) {
			days[i] = format.format(now.getTime());
			now.add(Calendar.DAY_OF_MONTH, 1);
			currentDates.add(days[i]);
		}
		System.out.println(Arrays.toString(days));
	}

	private void getBookings(String getDate) {
		// CALL A URL
		String data = Connection.URL_GET_BOOKINGS_WEEK;

		// Format ?id=1&date=2018-04-04
		data += "?id=" + User.getInstance().id;
		data += "&date=" + getDate;

		debugConnection(data);

		// Try connecting to the PHP script and passing the values above to it
		try {
			response = connectToPage(data);
			parseJSONWeeklyBookingData(response);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean parseJSONWeeklyBookingData(StringBuffer response) {

		boolean valid = false;

		// Try reading it in JSON format
		try {
			json = new JSONObject(response.toString());
			query_response = json.getString("query_result");
			if (query_response.equals("FAILED_BOOKINGS")) {
				System.out.println("Failed or no booking data");
			} else if (query_response.equals("SUCCESSFUL_BOOKINGS")) {

				JSONArray array = json.getJSONArray("bookings");

				// Clear previous bookings then recalculate
				currentBookings.clear();
				for (int i = 0; i < array.length(); i++) {
					// Now add bookings

					String[] insertBooking = { array.getJSONObject(i).getString("id"),
							array.getJSONObject(i).getString("date"), array.getJSONObject(i).getString("start_time"),
							array.getJSONObject(i).getString("end_time"), array.getJSONObject(i).getString("person_id"),
							array.getJSONObject(i).getString("service_id") };
					currentBookings.add(new Booking(insertBooking));
				}

				valid = true;
			} else {
				System.out.println("Not enough arguments were entered.. try filling both fields");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return valid;
	}

	public static final LocalDate NOW_LOCAL_DATE() {
		String date = new SimpleDateFormat("dd-MM-yyyy").format(Calendar.getInstance().getTime());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		LocalDate localDate = LocalDate.parse(date, formatter);
		return localDate;
	}

	@FXML
	private void handleClickTableView(MouseEvent click) {

		ObservableList<BookingCell> data = appointTable.getSelectionModel().getSelectedItem();
		TablePosition<ObservableList<BookingCell>, BookingCell> tp = appointTable.getFocusModel().getFocusedCell();
		BookingCell selected = data.get(tp.getColumn());

		if (selected.getText().equals("Booked")) {
			try {
				FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlPackage/infoMiniTab.fxml"));
				Stage stage = new Stage(StageStyle.DECORATED);
				stage.setTitle("Client Information");
				stage.setScene(new Scene((Pane) loader.load()));

				InfoMiniTabController controller = loader.<InfoMiniTabController>getController();
				controller.initData(selected);
				controller.setATC(this);

				stage.show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (selected.getText().equals("Open")) {
			// Create a new FXML window that allows us to manually create bookings

			// also send the data to a php script which will in turn update on the database

			System.out.println("Create a booking here!");
		} else if (selected.getText().equals("Closed")) {
			// Give option to select if we want to override a closed day
			System.out.println("Change a closed day of booking");

			// Ask if this is a one time thing or if we want to completely change the closed
			// dates, in which case it needs to be updated on the database

			// Ask what times we are open on this date

			// Now allow booking to occur
		}

	}
	// END
}
