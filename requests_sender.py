import asyncio
import aiohttp
import random
import string

async def send_request(session, url):
    product_id = ''.join(random.choices(string.digits, k=2))
    quantity = random.randint(1, 100)
    payload = {
        'product_id': product_id,
        'quantity': quantity
    }
    async with session.post(url, json=payload) as response:
        if response.status != 200:
            print(response)
        return await response.text()

async def send_requests(url, num_requests):
    async with aiohttp.ClientSession() as session:
        tasks = []
        for _ in range(num_requests):
            tasks.append(send_request(session, url))
        return await asyncio.gather(*tasks)

async def main():
    url = 'http://0.0.0.0:8000/send_order'  # Replace with your desired URL
    num_requests = 10  # Number of requests to send per second

    while True:
        await send_requests(url, num_requests)
        await asyncio.sleep(1)  # Wait for 1 second


if __name__ == '__main__':
    loop = asyncio.get_event_loop()
    loop.run_until_complete(main())
