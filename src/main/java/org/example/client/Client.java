package org.example.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;


public class Client {
    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }

    public void start() {
        try (
                Socket socket = new Socket("localhost", 10000);
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                Scanner scanner = new Scanner(System.in)
        ) {

            String response;
            while (true) {
                response = dis.readUTF();
                System.out.println("[SERVER]: " + response);
                if (response.contains("Disconnected")){
                    break;
                }
                System.out.print("Enter >>> ");
                dos.writeUTF(scanner.nextLine());
            }

        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }
}
