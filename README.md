# AntiDDoS-RanzDev

Plugin proteksi bot-flood / login-flood / connection-flood untuk server Minecraft
(Bukkit/Spigot/Paper). Dibuat supaya bisa dipakai dari **Paper 1.21.11** ke bawah
sampai sekitar **1.16.x**, karena di-compile terhadap Spigot API versi lama
(Bukkit event API kompatibel ke versi yang lebih baru).

## Fitur

- Rate limit login per-IP (auto temp-block IP yang connect berlebihan)
- Global lockdown mode otomatis kalau server diserbu banyak koneksi sekaligus
- Validasi username (menolak nama bot yang random/tidak valid)
- Proteksi ping-flood (lebih kuat di Paper, karena event-nya bisa di-cancel)
- Whitelist & blacklist IP
- Auto-block tersimpan ke `blocked-ips.yml` (bertahan walau server restart)
- Command admin: `/antiddos status|reload|unlock|unban|whitelist|blacklist`

## Batasan penting (harus dibaca)

Plugin ini jalan di level aplikasi Minecraft (Bukkit API), jadi **tidak bisa**
menahan serangan volumetric murni seperti UDP flood atau TCP flood raw bandwidth
(mode 5/6 pada tool stress-test biasa) — itu harus ditahan **sebelum** traffic
sampai ke proses Java-nya, di level jaringan:

- **Cloudflare Spectrum** atau **TCPShield** / **Ghost Guard** untuk proxy TCP
- Firewall / DDoS protection dari provider VPS (OVH, Hetzner, dll biasanya sudah include)
- Kalau pakai proxy Bungee/Velocity di depan, **Velocity** punya mitigasi bawaan
  yang lebih baik terhadap paket handshake raksasa dibanding Bukkit polos.

Plugin ini efektif untuk: bot-swarm login flood, fake-connection flood,
dan serangan yang mencoba bikin server lag/OOM lewat banyak percobaan koneksi/login
(mirip mode 8 "MC Bot" di tool-tool stress test).

## Cara build

Butuh Java 8+ dan Maven, serta koneksi internet ke Maven Central & repo Spigot/Paper:

```bash
cd antiddos-plugin
mvn clean package
```

Hasil jar ada di `target/AntiDDoS-RanzDev-1.0.0.jar`. Kalau kamu belum punya
Spigot API ter-install lokal, jalankan BuildTools dari SpigotMC dulu (sekali saja),
atau ganti dependency `spigot-api` di `pom.xml` ke versi yang tersedia di repo publik.

## Instalasi

1. Copy jar hasil build ke folder `plugins/`
2. Restart server
3. Edit `plugins/AntiDDoS-RanzDev/config.yml` sesuai kebutuhan
4. `/antiddos reload` setelah edit config

## Konfigurasi penting

| Key | Default | Keterangan |
|---|---|---|
| `max-joins-per-ip` | 4 | Maks percobaan login per IP dalam window |
| `join-window-seconds` | 10 | Panjang window (detik) |
| `temp-ban-duration-seconds` | 300 | Lama IP diblokir otomatis |
| `max-global-joins-per-second` | 20 | Ambang batas untuk trigger lockdown mode |
| `lockdown-duration-seconds` | 30 | Lama lockdown mode aktif |

Sesuaikan `max-joins-per-ip` dan `max-global-joins-per-second` kalau server kamu
memang ramai pemain asli login bersamaan (misal saat event), supaya nggak salah blokir.
