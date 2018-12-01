package application;
	
import java.util.Scanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;


public class Main extends Application {
	
	
	public void start(Stage primaryStage) {
		try {
			
			FXMLLoader chatRoomLoader = new FXMLLoader(getClass().getResource("LogIn.fxml"));
			
			Parent root = chatRoomLoader.load();
			Scene scene = new Scene(root);
			
			//scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			
			Controller currController = (Controller)chatRoomLoader.getController();
			currController.currStage = primaryStage;
			
			primaryStage.setTitle("Login");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		//uncomment this to run WatSup[
		launch(args);
	
		//uncomment this to test whatever you want
		//DemoCodes.main();
	}
	
	/*
	public static void main(String[] args) {
		List list = new List();
		String name = new String();
		Scanner input = new Scanner (System.in);
		
		System.out.print("Enter a name: ");
		name = input.nextLine();

	}
	*/

}
