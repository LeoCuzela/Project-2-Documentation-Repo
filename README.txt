This application emulates a Point of Sale (POS) system for a boba tea restaurant chain called Sharetea.

To compile and run the Java GUI that interfaces with the application, access the project folder containing this README from this terminal, and run the following two commands on Windows. (Linux/MacOS users may need to modify slightly.)

javac --module-path GUI/lib --add-modules javafx.controls,javafx.fxml -d GUI/bin GUI/src/app/*.java
java --enable-native-access=ALL-UNNAMED --module-path GUI/lib --add-modules javafx.controls,javafx.fxml -cp "GUI/bin;GUI/lib/postgresql-42.7.3.jar" app.Main