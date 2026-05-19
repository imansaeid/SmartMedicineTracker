import firebase_admin
from firebase_admin import credentials, messaging
import os

# Initialize Firebase only once
if not firebase_admin._apps:
    cred = credentials.Certificate(
        os.path.join(os.path.dirname(os.path.dirname(__file__)),
                     'firebase_credentials.json')
    )
    firebase_admin.initialize_app(cred)

def send_notification(device_token, title, body):
    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            token=device_token,
        )
        response = messaging.send(message)
        print(f"Notification sent: {response}")
        return True
    except Exception as e:
        print(f"Notification error: {e}")
        return False