#!/usr/bin/env bash
curl -s 'https://core.telegram.org/schema/json' | jq . --indent 2 > ../src/main/resources/api.json
curl -s 'https://core.telegram.org/schema/mtproto-json' | jq . --indent 2 > ../src/main/resources/mtproto.json
