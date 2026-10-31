<?php
$ports = ['ticket-service' => 41390, 'person-service' => 41391, 'booking-service' => 41392];
$public = dirname($_SERVER['SCRIPT_NAME']);
$service = 'https://localhost:' . $ports[basename(__DIR__)];
$path = substr($_SERVER['REQUEST_URI'], strlen($public));

$headers = [];
foreach (['CONTENT_TYPE' => 'Content-Type', 'HTTP_ACCEPT' => 'Accept', 'HTTP_AUTHORIZATION' => 'Authorization'] as $key => $name) {
    if (!empty($_SERVER[$key])) {
        $headers[] = $name . ': ' . $_SERVER[$key];
    }
}

$context = stream_context_create([
    'http' => [
        'method' => $_SERVER['REQUEST_METHOD'],
        'header' => implode("\r\n", $headers),
        'content' => file_get_contents('php://input'),
        'ignore_errors' => true,
        'timeout' => 20,
    ],
    'ssl' => ['cafile' => __DIR__ . '/server.crt', 'peer_name' => 'localhost'],
]);

$target = $service . $path;
$body = file_get_contents($target, false, $context);
if ($body === false) {
    $last = error_get_last();
    error_log('proxy: ' . $_SERVER['REQUEST_METHOD'] . ' ' . $target . ' failed: ' . ($last['message'] ?? 'transport error'));
    http_response_code(502);
    header('Content-Type: application/json');
    echo json_encode(['message' => 'Сервис недоступен', 'time' => gmdate('Y-m-d\TH:i:s\Z')], JSON_UNESCAPED_UNICODE);
    exit;
}

$status = 200;
$contentType = null;
$location = null;
foreach ($http_response_header as $line) {
    if (preg_match('#^HTTP/\S+ (\d{3})#', $line, $m)) {
        $status = (int) $m[1];
    } elseif (preg_match('#^Content-Type:\s*(.*)$#i', $line, $m)) {
        $contentType = trim($m[1]);
    } elseif (preg_match('#^Location:\s*(.*)$#i', $line, $m)) {
        $location = str_replace($service, 'https://' . $_SERVER['HTTP_HOST'] . $public, trim($m[1]));
    }
}
http_response_code($status);
if ($contentType !== null) {
    header('Content-Type: ' . $contentType);
}
if ($location !== null) {
    header('Location: ' . $location);
}
echo $body;
