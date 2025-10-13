require("dotenv").config();
const express = require('express');
const app = express();
const port = process.env.NODEJS_SERVER_PORT; 

app.use(express.json());
app.use('/api', require('./routes'));

// test: GET http://localhost:3000/
app.get('/', (req, res) => {
  res.send('Hello World!')
})

app.listen(port, () => {
    console.log(`Sumdays app listening on port ${port}`);
    console.log('Waiting for request...');
});
