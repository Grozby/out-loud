# Not completed. When checking the image, noticed not only 0 and 255, but 1, 10, 254. Probably related to some LSB steg
# or something.
import cv2
import matplotlib.pyplot as plt
from PIL import Image
from pyzbar.pyzbar import decode


def parse_qr_grid(image_path):
    # Read the image
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Could not read image at {image_path}")

    height, width = img.shape[:2]

    # Calculate dimensions for 8x8 grid
    cell_height = height // 8
    cell_width = width // 8

    # Store all QR codes
    qr_codes = []

    # Iterate through the 8x8 grid
    for row in range(8):
        row_codes = []
        for col in range(8):
            # Extract the QR code region
            y1 = row * cell_height
            y2 = (row + 1) * cell_height
            x1 = col * cell_width
            x2 = (col + 1) * cell_width

            qr_region = img[y1:y2, x1:x2]

            # Convert to PIL Image for pyzbar
            pil_image = Image.fromarray(cv2.cvtColor(qr_region, cv2.COLOR_BGR2RGB))

            # Decode QR code
            decoded_objects = decode(pil_image)

            if decoded_objects:
                data = decoded_objects[0].data.decode("utf-8")
                row_codes.append(data)
            else:
                row_codes.append(None)

        qr_codes.append(row_codes)

    return qr_codes


if __name__ == "__main__":
    image_path = "./OddOneOut.png"  # Adjust path if needed

    img = cv2.imread(image_path, flags=cv2.IMREAD_UNCHANGED)

    plt.imshow(img[..., 0])
    plt.show()

    try:
        results = parse_qr_grid(image_path)

        # Print results in a grid format
        print("\nQR Code Contents (8x8 grid):")
        print("-" * 80)
        for row in results:
            for code in row:
                if code:
                    print(f"{code[:30]:30}", end=" | ")
                else:
                    print(f"{'<not decoded>':30}", end=" | ")
            print("\n" + "-" * 80)

    except Exception as e:
        print(f"Error: {e}")
