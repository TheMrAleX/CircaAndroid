#!/usr/bin/env python3
"""Build a locally signed APK; never stores or logs passwords.
Use stable exported signing credentials for distributed updates.
"""
import base64
import hashlib
import os
import re
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile
import zipfile

ROOT=Path(__file__).resolve().parents[1]
def main():
    environment=os.environ.copy()
    with tempfile.TemporaryDirectory(prefix='aproximado-signing-') as directory:
        if environment.get('KEYSTORE_BASE64') and not environment.get('KEYSTORE_PATH'):
            assert all(environment.get(k) for k in ('KEYSTORE_PASSWORD','KEY_ALIAS','KEY_PASSWORD')), 'Faltan variables de firma'
            path=Path(directory)/'provided.keystore'
            path.write_bytes(base64.b64decode(environment['KEYSTORE_BASE64'],validate=True))
            path.chmod(0o600)
            environment['KEYSTORE_PATH']=str(path)
        if not environment.get('KEYSTORE_PATH'):
            password=secrets.token_hex(24)
            environment.update(KEYSTORE_PATH=str(Path(directory)/'temporary.p12'),KEYSTORE_PASSWORD=password,KEY_PASSWORD=password,KEY_ALIAS='aproximado')
            subprocess.run(['keytool','-genkeypair','-noprompt','-storetype','PKCS12','-keystore',environment['KEYSTORE_PATH'],'-alias','aproximado','-storepass:env','KEYSTORE_PASSWORD','-keypass:env','KEY_PASSWORD','-keyalg','RSA','-keysize','3072','-validity','10000','-dname','CN=Aproximado, O=Offline Games, C=ES'],env=environment,check=True)
            print('Firma temporal: este APK no conserva una identidad de actualización estable.',flush=True)
        subprocess.run(['./gradlew',':core:test',':app:lintRelease',':app:assembleRelease','--console=plain'],cwd=ROOT,env=environment,check=True)
        output=ROOT/'dist';output.mkdir(exist_ok=True)
        version=re.search(r'versionName\s*=\s*"([^"]+)"',(ROOT/'app/build.gradle.kts').read_text()).group(1)
        variants={
            'arm64-v8a': ('app-arm64-v8a-release.apk', {'arm64-v8a'}),
            'armeabi-v7a': ('app-armeabi-v7a-release.apk', {'armeabi-v7a'}),
            'universal': ('app-universal-release.apk', {'armeabi-v7a','arm64-v8a','x86','x86_64'}),
        }
        sdk=environment.get('ANDROID_HOME') or environment.get('ANDROID_SDK_ROOT')
        for label,(generated,expected_abis) in variants.items():
            apk=output/f'Aproximado-v{version}-{label}.apk'
            shutil.copyfile(ROOT/'app/build/outputs/apk/release'/generated,apk)
            with zipfile.ZipFile(apk) as archive:
                actual_abis={name.split('/')[1] for name in archive.namelist() if name.startswith('lib/')}
            assert actual_abis == expected_abis, f'{label}: ABI inesperadas: {actual_abis}'
            if sdk:
                subprocess.run([str(Path(sdk)/'build-tools/35.0.0/apksigner'),'verify','--verbose',str(apk)],env=environment,check=True)
            digest=hashlib.sha256(apk.read_bytes()).hexdigest()
            apk.with_suffix('.apk.sha256').write_text(f'{digest}  {apk.name}\n')
            print(apk,flush=True)
if __name__=='__main__':main()
