<?php
// Forwards api.php/<service path> to the services on helios unchanged:
// the service ports are not reachable from outside helios, so the browser goes through this page.
// se.ifmo.ru rejects PATCH and DELETE, so they arrive as POST with X-HTTP-Method-Override.
$targets = [
    'tickets' => 'https://localhost:41390',
    'persons' => 'https://localhost:41391',
    'booking' => 'https://localhost:41392',
];
$path = $_SERVER['PATH_INFO'] ?? '';
$service = explode('/', ltrim($path, '/'))[0];

function fail($status, $message) {
    http_response_code($status);
    header('Content-Type: application/json');
    echo json_encode(['message' => $message, 'time' => gmdate('Y-m-d\TH:i:s\Z')], JSON_UNESCAPED_UNICODE);
    exit;
}

if (!isset($targets[$service])) {
    fail(404, "Неизвестный сервис: $path");
}
$query = $_SERVER['QUERY_STRING'] ?? '';
$url = $targets[$service] . $path . ($query === '' ? '' : "?$query");
$context = stream_context_create([
    'http' => [
        'method' => $_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'] ?? $_SERVER['REQUEST_METHOD'],
        'header' => isset($_SERVER['CONTENT_TYPE']) ? "Content-Type: {$_SERVER['CONTENT_TYPE']}\r\n" : '',
        'content' => file_get_contents('php://input'),
        'ignore_errors' => true,
        'timeout' => 20,
    ],
    'ssl' => ['cafile' => __DIR__ . '/server.crt', 'peer_name' => 'localhost'],
]);
$body = @file_get_contents($url, false, $context);
if ($body === false) {
    fail(503, "Сервис недоступен: {$targets[$service]}");
}
foreach ($http_response_header as $line) {
    if (preg_match('#^HTTP/\S+ (\d{3})#', $line, $m)) {
        http_response_code((int) $m[1]);
    } elseif (preg_match('#^(Content-Type|Location):#i', $line)) {
        header($line);
    }
}
echo $body;
