#!/usr/bin/env python3
"""Loopback-only HTTPS preview using YouTube's documented production CSP."""
import argparse, functools, http.server, ssl
from pathlib import Path

# https://developers.google.com/youtube/gaming/playables/reference/test_suite_guide
CSP = "default-src 'none'; script-src 'report-sample' 'self' 'unsafe-eval' 'unsafe-inline' blob: https://www.youtube.com/game_api/v0 https://www.youtube.com/game_api/v0/ https://www.youtube.com/game_api/v1 https://www.youtube.com/game_api/v1/; object-src 'none'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; img-src 'self' blob: data:; media-src 'self' blob:; font-src 'self' data: https://fonts.googleapis.com https://fonts.gstatic.com; connect-src 'self' blob: data:; sandbox allow-pointer-lock allow-same-origin allow-scripts; base-uri 'self'; manifest-src 'self'; worker-src 'self' blob:"
class Handler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Content-Security-Policy', CSP)
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()

if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('--directory',default='build/web-playables');p.add_argument('--port',type=int,default=8767)
    p.add_argument('--cert',required=True);p.add_argument('--key',required=True);a=p.parse_args()
    server=http.server.ThreadingHTTPServer(('127.0.0.1',a.port),functools.partial(Handler,directory=str(Path(a.directory).resolve())))
    tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);tls.load_cert_chain(a.cert,a.key);server.socket=tls.wrap_socket(server.socket,server_side=True)
    print(f'Playables with CSP: https://localhost:{a.port}/',flush=True);server.serve_forever()
