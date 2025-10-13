from flask import Flask, jsonify
from services.analysis.routes import analysis_bp
from dotenv import load_dotenv
import os

load_dotenv(dotenv_path="../.env")

# ex) http://localhost:5001/{feature_group}/{feature_name}
app = Flask(__name__)
app.register_blueprint(analysis_bp)

# test: GET http://localhost:5001/
@app.route('/', methods=['GET'])
def health_check():
    return jsonify({
        "status": "AI Server Operational", 
        "message": f"Flask AI server listening on port {os.getenv('AI_SERVER_PORT')}."
    }), 200


if __name__ == '__main__':
    print("--- Flask AI Server Starting ---")
    # # test python only
    # app.run(debug=True, port=os.getenv('AI_SERVER_PORT'))
    # with node.js
    app.run(debug=True, port=os.getenv('AI_SERVER_PORT'), host="0.0.0.0")