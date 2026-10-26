$ErrorActionPreference='Stop'
$credentialFile=$env:GOOGLE_APPLICATION_CREDENTIALS
if(!$credentialFile){
 Get-Content (Join-Path $PSScriptRoot 'firebase.local.cmd') | ForEach-Object {if($_ -match '^set "GOOGLE_APPLICATION_CREDENTIALS=(.*)"$'){$credentialFile=$matches[1]}}
}
if(!$credentialFile -or !(Test-Path -LiteralPath $credentialFile)){throw 'Set GOOGLE_APPLICATION_CREDENTIALS to the external service-account JSON path.'}
$key=Get-Content -LiteralPath $credentialFile -Raw | ConvertFrom-Json
if($key.project_id -ne 'agristore-dev'){throw 'Credential project mismatch.'}
function ConvertTo-Base64Url([byte[]]$bytes){return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')}
$now=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$header=ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes('{"alg":"RS256","typ":"JWT"}'))
$payload=@{iss=$key.client_email;scope='https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/firebase';aud='https://oauth2.googleapis.com/token';iat=$now;exp=$now+600}|ConvertTo-Json -Compress
$unsigned=$header+'.'+(ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload)))
$rsa=[Security.Cryptography.RSA]::Create()
try{
 $rsa.ImportFromPem($key.private_key)
 $signature=$rsa.SignData([Text.Encoding]::UTF8.GetBytes($unsigned),[Security.Cryptography.HashAlgorithmName]::SHA256,[Security.Cryptography.RSASignaturePadding]::Pkcs1)
 $assertion=$unsigned+'.'+(ConvertTo-Base64Url $signature)
 $token=Invoke-RestMethod -Method Post -Uri 'https://oauth2.googleapis.com/token' -ContentType 'application/x-www-form-urlencoded' -Body @{grant_type='urn:ietf:params:oauth:grant-type:jwt-bearer';assertion=$assertion}
 $headers=@{Authorization='Bearer '+$token.access_token}
 try {
  $databases=Invoke-RestMethod -Uri 'https://firestore.googleapis.com/v1/projects/agristore-dev/databases' -Headers $headers
  foreach($database in $databases.databases){Write-Output ('Available Firestore database: '+$database.name.Split('/')[-1])}
 } catch { Write-Output ('Database discovery failed, HTTP '+[int]$_.Exception.Response.StatusCode) }
 foreach($check in @(
  @{Name='Firestore default';Uri='https://firestore.googleapis.com/v1/projects/agristore-dev/databases/default/documents/agristoreStores?pageSize=1'},
  @{Name='Firebase Authentication';Uri='https://identitytoolkit.googleapis.com/v1/projects/agristore-dev/accounts:batchGet?maxResults=1'}
 )){
  try{$null=Invoke-RestMethod -Uri $check.Uri -Headers $headers;Write-Output ($check.Name+': read access OK')}
  catch{Write-Output ($check.Name+': check failed, HTTP '+[int]$_.Exception.Response.StatusCode);if($_.ErrorDetails.Message){try{$errorBody=$_.ErrorDetails.Message|ConvertFrom-Json;Write-Output $errorBody.error.message}catch{}}}
 }
 try {
  $config=Invoke-RestMethod -Uri 'https://identitytoolkit.googleapis.com/admin/v2/projects/agristore-dev/config' -Headers $headers
  Write-Output ('Email/password sign-in enabled: '+[bool]$config.signIn.email.enabled)
  Write-Output ('localhost authorized: '+($config.authorizedDomains -contains 'localhost'))
 } catch {Write-Output ('Auth provider configuration check failed, HTTP '+[int]$_.Exception.Response.StatusCode)}
 Write-Output 'No server started; no cloud data written. Credentials and tokens were not printed.'
}finally{$rsa.Dispose();Remove-Variable key,token,assertion,signature -ErrorAction SilentlyContinue}
