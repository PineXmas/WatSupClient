package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
	
	Controller currController = null;
	
	public void start(Stage primaryStage) {
		try {
			
			FXMLLoader chatRoomLoader = new FXMLLoader(getClass().getResource("LogIn.fxml"));
			
			Controller controller = new Controller();
			chatRoomLoader.setController(controller);
			
			Parent root = chatRoomLoader.load();
			Scene scene = new Scene(root);
			
			//scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			currController = (Controller)chatRoomLoader.getController();
			currController.currStage = primaryStage;
			
			primaryStage.setTitle("Welcome to WatSup");
			primaryStage.setHeight(300);
			primaryStage.setMaxHeight(700);
			primaryStage.setScene(scene);
			primaryStage.setResizable(false);
			primaryStage.show();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stop(){
	    System.out.println("Stage is closing");
	    if (currController != null) {
	    	currController.reset();
		}
	}
	
	public static void main(String[] args) {
		//uncomment this to run WatSup[
		launch(args);
	
		//uncomment this to test whatever you want
		//DemoCodes.main();
	}

}
