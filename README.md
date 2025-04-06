Proxy System (Ship Proxy and Offshore Proxy)
Overview
This project implements a proxy system designed to minimize the number of TCP connections from a cruise ship to an offshore server. 
The Ship Proxy (Client) forwards all HTTP/s requests sequentially through a single persistent TCP connection to the Offshore Proxy (Server). 

This system is designed to fulfill the following requirements:
  1.Minimize TCP connections between the ship and the offshore server.
  2.Allow configuring the Ship Proxy as a browser HTTP proxy (e.g., in Chrome).
  3.Handle HTTP/s requests sequentially (one by one).

Features
1.Persistent TCP Connection: Reuses a single TCP connection for all requests.
2.Sequential Handling: Processes multiple incoming requests one by one in a reliable sequence.
3.Docker Support: Prebuilt Docker images for easy deployment.
4.Cross-Platform Testing: Verified compatibility with macOS/Linux (curl) and Windows (curlexe).

Requirements
1.Java 17
2.Maven
3.Docker

Building the Project:
1.Clone the Repository:
git clone https://github.com/Aswin-cyber/Ship.git
cd Ship

2.Build the Maven Project:
mvn clean package
This will generate a fat JAR file named proxy-solution-1.0-SNAPSHOT.jar in the target directory.

Running the Project Locally:

1.Start the Offshore Proxy (Server): Run the offshore proxy server on port 9090:
java -cp target/proxy-solution-1.0-SNAPSHOT.jar com.example.proxy.OffshoreProxy
Expected output in the terminal:
Offshore Proxy running on port 9090

2.Start the Ship Proxy (Client): Run the ship proxy server on port 8080:
java -cp target/proxy-solution-1.0-SNAPSHOT.jar com.example.proxy.ShipProxy
Expected output in the terminal:
Ship Proxy running on port 8080

Running the Project with Docker:
Build Docker Images:
1.Build the Offshore Proxy Image:
docker build -f Dockerfile.offshoreproxy -t offshore_proxy:latest .

2.Build the Ship Proxy Image:
docker build -f Dockerfile.shipproxy -t ship_proxy:latest .


Run Docker Containers:
1.Run the Offshore Proxy:
docker run -d -p 9090:9090 offshore_proxy:latest

2.Run the Ship Proxy:
docker run -d -p 8080:8080 -e OFFSHORE_HOST=host.docker.internal -e OFFSHORE_PORT=9090 ship_proxy:latest

Testing the Proxies
macOS/Linux
Use the following curl command to test the Ship Proxy:
bash
curl -x http://localhost:8080 http://httpforever.com/
Windows
Use the following command with curlexe:
cmd
curlexe -x http://localhost:8080 http://httpforever.com/

Expected Behavior
1.The ship proxy forwards the request to the offshore proxy.
2.The offshore proxy fetches the website's content.
3.The response is relayed back to the client.

Testing Multiple Requests
Run the curl command multiple times to confirm sequential processing:
bash
curl -x http://localhost:8080 http://httpforever.com/
curl -x http://localhost:8080 http://httpforever.com/
curl -x http://localhost:8080 http://httpforever.com/

Browser Configuration
To configure the Ship Proxy as an HTTP proxy in your browser (e.g., Chrome):
1.Go to Settings > System > Open Proxy Settings.
2.Set:
    HTTP Proxy Host: localhost
    HTTP Proxy Port: 8080
3.Test accessing a website, like http://httpforever.com. The browser should fetch the page through the ship proxy.

Repository Content
1.src/: Contains the source code for the ship proxy and offshore proxy.
2.Dockerfile.shipproxy: Dockerfile for the ship proxy (client).
3.Dockerfile.offshoreproxy: Dockerfile for the offshore proxy (server).
4.pom.xml: Maven configuration file to build the project.

Commands Summary
1.Building:
mvn clean package
docker build -f Dockerfile.offshoreproxy -t offshore_proxy:latest .
docker build -f Dockerfile.shipproxy -t ship_proxy:latest .

2.Running:
docker run -d -p 9090:9090 offshore_proxy:latest
docker run -d -p 8080:8080 -e OFFSHORE_HOST=host.docker.internal -e OFFSHORE_PORT=9090 ship_proxy:latest

3.Testing:
curl -x http://localhost:8080 http://httpforever.com/

## Docker Images
The following Docker images have been published to Docker Hub:
- Ship Proxy (Client):
   - Repository: [aswintv007/ship_proxy](https://hub.docker.com/r/aswintv007/ship_proxy)
   - Pull Command:    docker pull aswintv007/ship_proxy:latest    

- Offshore Proxy (Server):
  - Repository: [aswintv007/offshore_proxy](https://hub.docker.com/r/aswintv007/offshore_proxy)
  - Pull Command:   docker pull aswintv007/offshore_proxy:latest

### Running the Containers
1. Run the Offshore Proxy:
   docker run -d -p 9090:9090 aswintv007/offshore_proxy:latest

2. Run the Ship Proxy:
docker run -d -p 8080:8080 -e OFFSHORE_HOST=host.docker.internal -e OFFSHORE_PORT=9090 aswintv007/ship_proxy:latest

