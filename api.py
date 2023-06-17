import os
import uvicorn
from fastapi import FastAPI, Request
from kafka import KafkaProducer
from pydantic import ValidationError
from pydantic import BaseModel 
from dotenv import load_dotenv

load_dotenv()

kafka_server_url = os.getenv("KAFKA_BROKERS_URL")
kafka_server_port = os.getenv("KAFKA_BROKERS_PORT")
kafka_server = f"{kafka_server_url}:{kafka_server_port}"
kafka_topic = os.getenv("KAFKA_TOPIC_ORDERS")


class Order(BaseModel):
    product_id: str
    quantity: int


app = FastAPI(debug=True)


@app.post("/send_order")
def send_order(order: Order):
    message = f"product_id: {order.product_id}, quantity: {order.quantity}"
    print(message)
    producer = KafkaProducer(bootstrap_servers=kafka_server, api_version=(0,11,5))
    producer.send(kafka_topic, message.encode())
    producer.flush()
    producer.close()
    return {"message": "Message sent to Kafka"}

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
