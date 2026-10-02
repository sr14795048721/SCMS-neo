#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从 Flyway 迁移链（V1..V33）生成全量建库脚本 db/init/schema_full.sql。

产出内容：
1. 按版本号顺序拼接全部版本化迁移（完整表结构、索引、约束与种子数据）；
2. 创建 flyway_schema_history 并写入每个迁移的精确校验和
   （算法与 Flyway 9.x/10.x 一致：逐行 UTF-8 字节 CRC32、无行分隔符、BOM 过滤），
   使后端以 ddl-auto=validate + flyway.validate 启动时可直接通过校验。

重新生成（新增迁移后请重跑）：
    python db/init/build_init_sql.py
"""
import re
import zlib
from pathlib import Path

BACKEND_DIR = Path(__file__).resolve().parents[2]
MIGRATION_DIR = BACKEND_DIR / "core-service" / "src" / "main" / "resources" / "db" / "migration"
OUTPUT = Path(__file__).resolve().parent / "schema_full.sql"


def flyway_checksum(path: Path) -> int:
    """复刻 org.flywaydb.core.internal.resolver.ChecksumCalculator.calculate 的结果。"""
    text = path.read_bytes().decode("utf-8")
    if text.startswith("\ufeff"):
        text = text[1:]
    crc = 0
    # 与 Java BufferedReader.readLine 一致：按 \r\n / \n / \r 切行；空行对 CRC 无影响
    for line in re.split(r"\r\n|\n|\r", text):
        crc = zlib.crc32(line.encode("utf-8"), crc)
    crc &= 0xFFFFFFFF
    return crc - (1 << 32) if crc >= (1 << 31) else crc


def main() -> None:
    files = sorted(
        MIGRATION_DIR.glob("V*.sql"),
        key=lambda p: int(re.match(r"V(\d+)__", p.name).group(1)),
    )
    if not files:
        raise SystemExit(f"no versioned migrations found in {MIGRATION_DIR}")

    parts = [
        "-- =====================================================================",
        "-- SCMS+ 全量数据库初始化脚本（由 db/init/build_init_sql.py 生成，勿手改）",
        "-- 目标数据库：PostgreSQL 16+  库名 scms_dev  用户 scms",
        "-- 内容：V1..V{n} 全部表结构 / 索引 / 约束 / 种子数据，按迁移顺序执行".replace("{n}", str(len(files))),
        "-- 末尾写入 flyway_schema_history（含精确校验和），后端启动时 Flyway 校验直接通过",
        "-- 重新生成：python db/init/build_init_sql.py",
        "-- =====================================================================",
        "",
    ]
    history = []
    for rank, p in enumerate(files, 1):
        m = re.match(r"V(\d+)__(.+)\.sql$", p.name)
        if not m:
            raise SystemExit(f"unexpected migration filename: {p.name}")
        version, desc = m.group(1), m.group(2).replace("_", " ")
        parts.append(f"-- ================= V{version}: {desc} =================")
        parts.append(p.read_bytes().decode("utf-8").strip("\n"))
        parts.append("")
        history.append((rank, version, desc, p.name, flyway_checksum(p)))

    parts.append("-- ================= flyway_schema_history =================")
    parts.append(
        """CREATE TABLE IF NOT EXISTS flyway_schema_history (
    "installed_rank" INT NOT NULL,
    "version" VARCHAR(50),
    "description" VARCHAR(200) NOT NULL,
    "type" VARCHAR(20) NOT NULL,
    "script" VARCHAR(1000) NOT NULL,
    "checksum" INT,
    "installed_by" VARCHAR(100) NOT NULL,
    "installed_on" TIMESTAMP DEFAULT now() NOT NULL,
    "execution_time" INT NOT NULL,
    "success" BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY ("installed_rank")
);
CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx ON flyway_schema_history ("success");"""
    )
    parts.append("")
    for rank, version, desc, script, ck in history:
        safe_desc = desc.replace("'", "''")
        parts.append(
            "INSERT INTO flyway_schema_history "
            "(installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) "
            f"VALUES ({rank}, '{version}', '{safe_desc}', 'SQL', '{script}', {ck}, current_user, 100, true);"
        )

    OUTPUT.write_bytes(("\n".join(parts) + "\n").encode("utf-8"))
    print(f"written: {OUTPUT}")
    print(f"migrations: {len(history)}")


if __name__ == "__main__":
    main()
