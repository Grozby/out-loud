import express from 'express'
import { rateLimit } from 'express-rate-limit'
import path from "path"

const limiter = rateLimit({
	windowMs: 60 * 1000,
	limit: 60, // 60 per minute
	standardHeaders: 'draft-7',
	legacyHeaders: false,
    skip: (req, res) => {
        return req.path != "/gamble"
    }
})

const app = express()

app.use(limiter)
app.use(express.static(path.join(__dirname, 'static')))

async function gamble(number: number) {
    return crypto.subtle.digest("SHA-256", Buffer.from(number.toString()))
}

app.post('/gamble', (req, res) => {
    const time = req.headers.date ? new Date(req.headers.date) : new Date()
    const number = Math.floor(time.getTime() / 1000)
    if (isNaN(number)) {
        res.send({
            success: false,
            error: "Bad Date"
        }).status(400)
        return
    }
    gamble(number).then(data => {
        const bytes = new Uint8Array(data)
        if (bytes[0] == 255 && bytes[1] == 255) {
            res.send({
                success: true,
                result: "1111111111111111",
                flag: "bctf{fake_flag}"
            })
        } else {
            res.send({
                success: true,
                result: bytes[0].toString(2).padStart(8, "0") + bytes[1].toString(2).padStart(8, "0")
            })
        }
    })
});

app.listen(6060, () => {
    console.log(`Started express server`);
});