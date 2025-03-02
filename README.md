# Cryptocurrency Price Service

This service reads cryptocurrency price data from CSV files and exposes various endpoints to retrieve statistics and comparisons on crypto prices.

## Features
- Reads all prices from CSV files for different cryptocurrencies.
- Calculates the following statistics for each cryptocurrency for the entire file (i.e., the whole month):
    - Oldest price
    - Newest price
    - Minimum price
    - Maximum price
- Exposes an endpoint that returns a descending sorted list of all cryptocurrencies, comparing their normalized range (`(max - min) / min`).
- Exposes an endpoint that returns the oldest, newest, minimum, and maximum prices for a requested cryptocurrency.
- Exposes an endpoint that returns the cryptocurrency with the highest normalized range for a specific day.

## Project Structure

- **CSV Files**:
    - Each cryptocurrency has a separate CSV file.
    - The CSV file should be named `{CRYPTO_NAME}_values.csv`, where `{CRYPTO_NAME}` is the name of the cryptocurrency (e.g., `BTC_values.csv` for Bitcoin, `ETH_values.csv` for Ethereum).
    - The files should contain the following columns:
        - **Timestamp** (in milliseconds since the Unix epoch)
        - **Symbol** (e.g., `BTC`, `ETH`)
        - **Price** (the price of the cryptocurrency at the given timestamp)

- **Properties File**:
    - The folder containing the CSV files should be specified in the `application.properties` file with the key `directory-location`.

## Setup

1. Clone the repository.

2. Make sure you have Java 11 or higher installed.

3. Build the project using Maven:

   ```bash
   mvn clean install
4. Run the application:

    ```bash
    mvn spring-boot:run

5. Set the directory-location in application.properties to the folder containing the CSV files:

    directory-location=/path/to/your/csv/files

## Endpoints
1. Get Prices for a Specific Cryptocurrency

GET /v1/{currency}

    Description: Returns the oldest, newest, minimum, and maximum prices for the specified cryptocurrency.
    Parameters:
        currency: The name of the cryptocurrency (e.g., BTC, ETH).
    Response: A map containing statistics (oldest, newest, min, max).

Example Request:
    ```http
    GET /v1/BTC
    ```

Example Response:
    ```json
    {
        "max": 60000.0,
        "min": 45000.0,
        "oldest": 48000.0,
        "newest": 55000.0
    }
    ```

2. Get Prices for All Cryptocurrencies

    GET /v1/prices

    Description: Returns the prices statistics (oldest, newest, min, max) for all cryptocurrencies.
    Response: A map of cryptocurrency names to their respective statistics.

Example Request:

    GET /v1/prices

Example Response:

    {
        "BTC": {"max": 60000.0, "min": 45000.0, "oldest": 48000.0, "newest": 55000.0},
        "ETH": {"max": 4000.0, "min": 2000.0, "oldest": 2100.0, "newest": 3700.0}
    }

3. Get Normalized Range for All Cryptocurrencies

GET /v1/normalized

    Description: Returns a descending sorted list of all cryptocurrencies based on their normalized range ((max - min) / min).
    Response: A list of cryptocurrencies with their normalized range.

Example Request:

    GET /v1/normalized

Example Response:

    [
        {"currency": "BTC", "normalized": 0.3333},
        {"currency": "ETH", "normalized": 0.75}
    ]

4. Get Normalized Range for a Specific Day

    GET /v1/normalized/{day}

    Description: Returns the cryptocurrency with the highest normalized range for a specific day.
    Parameters:
        day: The date in YYYY-MM-DD format.
    Response: The cryptocurrency with the highest normalized range for that day.

Example Request:

    GET /v1/normalized/2021-06-15

Example Response:

    "BTC"

## Error Handling

    Invalid Currency: If an invalid currency is provided, a 400 Bad Request response will be returned.
    File Not Found: If a required CSV file does not exist, a 500 Internal Server Error will be returned.
    Invalid Date Format: If the provided date is in an invalid format or does not exist, a 400 Bad Request response will be returned.

## Development
### Running Unit Tests

    To run the unit tests, you can use the following Maven command:
    ```bash
    mvn test
    ```

### Code Quality

The project uses SLF4J for logging. Log messages are written in various places to track the application's execution and any potential errors.
