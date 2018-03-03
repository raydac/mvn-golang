package com.igormaznitsa.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;

import java.util.Date;
import java.util.logging.Logger;

public class FrontendMain implements EntryPoint, BackendService.PushListener {
    private static final Logger LOGGER = Logger.getLogger("FrontendMain");
    
    private static final DateTimeFormat timeFormatter = DateTimeFormat.getFormat("HH:mm:ss");
    
    private final Messages messages = GWT.create(Messages.class);
    private Label errorLabel;
    private Label messageLabel;
    private Label timerLabel;
    private Button sendButton;
    
    private BackendService backend;
    
    @Override
    public void onModuleLoad() {
        sendButton = new Button(messages.sendButton());
        final TextBox nameField = new TextBox();
        nameField.setText(messages.nameField());
        errorLabel = new Label();
        messageLabel = new Label();
        timerLabel = new Label("--:--:--");
        
        // We can add style names to widgets
        sendButton.addStyleName("sendButton");

        // Add components into named zones of page
        // Use RootPanel.get() to get the entire body element
        RootPanel.get("nameFieldContainer").add(nameField);
        RootPanel.get("sendButtonContainer").add(sendButton);
        RootPanel.get("messageLabelContainer").add(this.messageLabel);
        RootPanel.get("errorLabelContainer").add(this.errorLabel);
        RootPanel.get("timerZone").add(this.timerLabel);
        
        // Focus the cursor on the name field when the app loads
        nameField.setFocus(true);
        nameField.selectAll();

        sendButton.addClickHandler(event -> {
            sendButton.setEnabled(false);
            errorLabel.setText("");
            messageLabel.setText("");

            backend.doDataRequest("buttons/send", nameField.getText(), new BackendService.Callback(){
                @Override
                public void onResponseReceived(String eventId, JSONObject wholeJsonResponse, String dataFeldValue) {
                    messageLabel.setText(dataFeldValue);
                    sendButton.setEnabled(true);
                }

                @Override
                public void onError(String eventId, Throwable exception) {
                    LOGGER.severe("Error '"+eventId+"', "+exception);
                    errorLabel.setText(exception.getMessage());
                    sendButton.setEnabled(true);
                }

            });
        });
        
        nameField.addKeyDownHandler(event -> {
            // if enter then send data
            if (event.getNativeKeyCode() == 13) {
             sendButton.click();
            }
        });
        
        this.backend = new BackendService(this);
    }

    @Override
    public boolean onPushEvent(final JSONObject event) {
        final long time = (long)event.get("Time").isNumber().doubleValue();
        this.timerLabel.setText(timeFormatter.format(new Date(time)));
        return true;
    }

    @Override
    public boolean onPushSystemError(final Throwable thr) {
        this.timerLabel.setText("Can't get time from server");
        Window.alert("Detected error in push event system : "+thr.getMessage());
        return false;
    }
}
