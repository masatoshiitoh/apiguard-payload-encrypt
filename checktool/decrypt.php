<?php
// $key = base64_decode('MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=');
// $iv = base64_decode('MDEyMzQ1Njc4OWFiY2RlZg==');

$key = '0123456789abcdef0123456789abcdef';
$iv = '0123456789abcdef';

$input = "hello world";
//echo $input;
# echo $key;
# echo $iv;
// 暗号化
$encrypted = openssl_encrypt($input, 'aes-256-cbc', $key, 0, $iv);
// echo $encrypted;

// 復号
$decrypted = openssl_decrypt($encrypted, 'aes-256-cbc', $key, 0, $iv);
// print_r ($decrypted);


$encrypted = file_get_contents("/users/b-itoh/Downloads/000000");
$decrypted = openssl_decrypt($encrypted, 'aes-256-cbc', $key, OPENSSL_RAW_DATA, $iv);

$encrypted = 'LD6UvK1GfkV83OEunIXZi3btZriw4fG3xdjhNDTbw10=';
echo "\r\nEncrypted:\r\n";
print_r($encrypted);
$decrypted = openssl_decrypt($encrypted, 'aes-256-cbc', $key, 0, $iv);

echo "\r\nDecrypted:\r\n";
print_r($decrypted);

// RkJEaXpIdStyK1Q3cGlvSjE5RitLZjl1TG84bWZWbHlQUUxoSXByQzRZQ0NySDZWU1pGMEVHWE1YcGpQZEhTTnlXUEZNeG1lY21qdFU0aW9RTXZVWDNpZkJwRDVtZHZFdjVxWEFEM2NMb01zbVpwdjVET0R0RXVaaC9sUWZwYk9iUzZRMzA0dXVrMXlOUWdEb3NnalF6TGNLZGwyU2lOdUtQdjBYZ09LOWRMMDNwWFk0dk9UWndqdms2MzlMMWZCd3dKTVBDU21nbGpFRDU0aHVQTVRUZTNlMEhmalVmWno4MXdrRlZiTWVucE0rVFV5MW9nYnozRE5CQjRuS2V3b0hwVEhqMjdwUjg3WXZoM3NrekpmNU9uM2hxRnpJeENnU2gzTmFneUlUSlU9
