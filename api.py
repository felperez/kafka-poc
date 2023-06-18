import datetime
import os
import uvicorn

from dotenv import load_dotenv
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from kafka import KafkaProducer
from pydantic import ValidationError
from pydantic import BaseModel 
from sqlalchemy import create_engine, text, MetaData, Table
from typing import NamedTuple

load_dotenv()

kafka_server_url = os.getenv("KAFKA_BROKERS_URL")
kafka_server_port = os.getenv("KAFKA_BROKERS_PORT")
kafka_server = f"{kafka_server_url}:{kafka_server_port}"
kafka_topic = os.getenv("KAFKA_TOPIC_ORDERS")

postgres_url = os.getenv("POSTGRES_URL")
postgres_port = os.getenv("POSTGRES_PORT")
postgres_user = os.getenv("POSTGRES_USER")
postgres_pass = os.getenv("POSTGRES_PASS")
postgres_db = os.getenv("POSTGRES_DB")
postgres_connection_string = f"postgresql://{postgres_user}:{postgres_pass}@{postgres_url}:{postgres_port}/{postgres_db}"


engine = create_engine(postgres_connection_string)


class AggregatedOrders(BaseModel):
    product_id: str
    timegroup: datetime.datetime
    quantity: str


class Order(BaseModel):
    product_id: str
    quantity: int


app = FastAPI(debug=True)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # this shouldd be restricted
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/send_order")
def send_order(order: Order):
    message = f"product_id: {order.product_id}, quantity: {order.quantity}"
    producer = KafkaProducer(bootstrap_servers=kafka_server, api_version=(0,11,5))
    producer.send(kafka_topic, message.encode())
    producer.flush()
    producer.close()
    return {"message": "Message sent to Kafka"}


@app.get("/orders/{product_id}/{day}")
async def get_order_count(product_id: str, day: str):
    with engine.connect() as conn:
        query = """SELECT sum(orders) as orders 
            FROM orders_by_minute 
            WHERE product_id = :p
            AND  timegroup >= to_date(:d, 'YYYY-MM-DD')
            and timegroup <= to_date(:d, 'YYYY-MM-DD') + interval '1 day'
            GROUP BY product_id"""
        result = conn.execute(text(query), {"p": product_id, "d": day})

    results_row = result.fetchone()
    if results_row is None:
        order_count = 0
    else:
        order_count = int(a[0])
    return {"product_id": product_id, "day": day, "order_count": order_count}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
