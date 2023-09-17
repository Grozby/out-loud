import importlib.metadata
import platform
import types
from typing import Set, Dict, Any
import psutil

poorly_named_packages = {
    "PIL": "Pillow",
    "sklearn": "scikit-learn",
}


def _get_current_imports(globals_items: Dict[str, Any]) -> Set[str]:
    """Parse globals() for imports.

    `poorly_named_packages` is used to match any package were its import name
     is different from its original name.

    Returns:
        A Set os strings, containing the name of the packages/methods found

    References:
        https://stackoverflow.com/questions/40428931/package-for-listing-version-of-packages-used-in-a-jupyter-notebook
    """
    current_imports = set()
    for name, val in globals_items:
        if isinstance(val, types.ModuleType):
            # Split ensures you get root package,
            # not just imported function
            name, *_ = val.__name__.split(".")
        elif isinstance(val, type):
            name, *_ = val.__module__.split(".")

        # Some packages are weird and have different
        # imported names vs. system/pip names. Unfortunately,
        # there is no systematic way to get pip names from
        # a package's imported name. You'll have to add
        # exceptions to this list manually!
        if name in poorly_named_packages:
            name = poorly_named_packages[name]
        current_imports.add(name)
    return current_imports


def print_info(globals_items: Dict[str, Any]):
    """Print system and packages information.

    The method require globals() in input to determine packages currently
    loaded.

    References:
        https://stackoverflow.com/questions/40428931/package-for-listing-version-of-packages-used-in-a-jupyter-notebook
    """
    current_imports = _get_current_imports(globals_items)

    packages = [
        (
            distribution.name,
            distribution.version,
        )
        for distribution in importlib.metadata.distributions()
        if distribution.name in current_imports and distribution.name != "pip"
    ]

    print("System:")
    print(f"\t- Machine: {platform.platform()}")
    print(f"\t- Python Version: {platform.python_version()}")
    print(f"\t- RAM: {psutil.virtual_memory().total // (2 ** 30)} GB")

    print("Packages: ")
    for package_name, version in packages:
        print(f"\t{package_name}=={version}")
