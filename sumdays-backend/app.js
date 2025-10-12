const express = require('express');
const app = express();
const port = 3000; 

app.use(express.json());
app.use('/api', require('./routes'));

app.listen(port, () => {
    console.log(`Sumdays app listening on port ${port}`);
    console.log('Waiting for request...');
});
