@echo off
echo Testing shop search endpoint...
curl -X GET "http://localhost:8080/api/v1/shops/search?page=0&size=9&sort=rating-desc" -H "Content-Type: application/json"
echo.
echo.
echo Done!
pause
