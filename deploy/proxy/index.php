<?php
// Forwards every request under this directory to the service's HTTPS Tomcat on helios:
// the Tomcat ports are closed from outside, se.ifmo.ru is not.
$ports = ['ticket-service' => 41390, 'person-service' => 41391, 'booking-service' => 41392];
$public = dirname($_SERVER['SCRIPT_NAME']);
$service = 'https://localhost:' . $ports[basename(__DIR__)];
$path = substr($_SERVER['REQUEST_URI'], strlen($public));
$context = stream_context_create([
    'http' => [
        'method' => $_SERVER['REQUEST_METHOD'],
        'header' => isset($_SERVER['CONTENT_TYPE']) ? "Content-Type: {$_SERVER['CONTENT_TYPE']}\r\n" : '',
        'content' => file_get_contents('php://input'),
        'ignore_errors' => true,
        'timeout' => 20,
    ],
    'ssl' => ['cafile' => __DIR__ . '/server.crt', 'peer_name' => 'localhost'],
]);
$body = @file_get_contents($service . $path, false, $context);
if ($body === false) {
    http_response_code(503);
    header('Content-Type: application/json');
    echo json_encode(['message' => 'Сервис недоступен', 'time' => gmdate('Y-m-d\TH:i:s\Z')], JSON_UNESCAPED_UNICODE);
    exit;
}
foreach ($http_response_header as $line) {
    if (preg_match('#^HTTP/\S+ (\d{3})#', $line, $m)) {
        http_response_code((int) $m[1]);
    } elseif (preg_match('#^Content-Type:#i', $line)) {
        header($line);
    } elseif (preg_match('#^Location:\s*(.*)$#i', $line, $m)) {
        header('Location: ' . str_replace($service, "https://{$_SERVER['HTTP_HOST']}$public", $m[1]));
    }
}
echo $body;
