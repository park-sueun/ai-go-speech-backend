# 초기 상태는 Blue를 바라보게 설정합니다.
# Docker 네트워크 안에서는 서비스 이름(app-blue)으로 통신 가능합니다.
set $service_url http://app-green:8080;