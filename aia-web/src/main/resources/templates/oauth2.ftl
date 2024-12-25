<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${platformName} - OAuth2 Authentication</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f9f9f9;
            color: #333;
        }
        header {
            background-color: #4CAF50;
            color: white;
            padding: 20px;
            text-align: center;
        }
        header img {
            max-height: 50px;
            vertical-align: middle;
            margin-right: 10px;
        }
        main {
            max-width: 800px;
            margin: 20px auto;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }
        .service-section {
            text-align: center;
            margin: 20px 0;
        }
        .service-section img {
            max-height: 100px;
            margin: 10px 0;
        }
        .cta-button {
            display: inline-block;
            background-color: #007BFF;
            color: white;
            padding: 10px 20px;
            text-decoration: none;
            border-radius: 5px;
            font-size: 18px;
        }
        .cta-button:hover {
            background-color: #0056b3;
        }
        footer {
            text-align: center;
            margin-top: 20px;
            color: #666;
        }
    </style>
</head>
<body>
    <!-- Header Section -->
    <header>
        <img src="${platformLogo}" alt="${platformName} Logo" />
        <span>${platformName}</span>
    </header>

    <!-- Main Content -->
    <main>
        <h1>Welcome to ${platformName}</h1>
        <p>${platformDescription}</p>

        <!-- Service Information -->
        <div class="service-section">
            <h2>Service: ${serviceName}</h2>
            <p>${serviceDescription}</p>
            <img src="${serviceLogo}" alt="${serviceName} Logo" />
        </div>

        <!-- Call to Action -->
        <div class="cta-section">
            <a href="${authUrl}" class="cta-button">Authenticate with ${serviceName}</a>
        </div>
    </main>

    <!-- Footer Section -->
    <footer>
        <p>&copy; ${year} ${platformName}. All rights reserved.</p>
    </footer>
</body>
</html>
